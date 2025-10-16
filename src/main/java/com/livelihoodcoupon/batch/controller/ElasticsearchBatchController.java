package com.livelihoodcoupon.batch.controller;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import com.livelihoodcoupon.common.exception.ErrorCode;
import com.livelihoodcoupon.common.response.CustomApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/admin/batch/es")
@RequiredArgsConstructor
@Profile("!test")
public class ElasticsearchBatchController {

	private final JobLauncher jobLauncher;
	private final ResourcePatternResolver resourcePatternResolver;
	private final ElasticsearchClient elasticsearchClient;

	@Qualifier("placeCsvToEsJob")
	private final Job placeCsvToEsJob;

	@Qualifier("placeCsvToEsIncrementalAddJob")
	private final Job placeCsvToEsIncrementalAddJob;

	@Qualifier("parkingLotCsvToEsJob")
	private final Job parkingLotCsvToEsJob;

	@Value("${batch.csv.file.path}")
	private String csvFilePath;

	@PostMapping("/parkinglots-index")
	public ResponseEntity<CustomApiResponse<?>> createParkingLotIndex() {
		String indexName = "parkinglots";
		try {
			boolean indexExists = elasticsearchClient.indices().exists(r -> r.index(indexName)).value();
			if (indexExists) {
				log.info("인덱스 '{}'가 이미 존재합니다.", indexName);
				return ResponseEntity.ok(CustomApiResponse.success("인덱스가 이미 존재합니다."));
			}

			Resource mappingResource = new ClassPathResource("elasticsearch/parkinglots-mapping.json");
			try (InputStream mappingInputStream = mappingResource.getInputStream()) {
				CreateIndexRequest createIndexRequest = new CreateIndexRequest.Builder()
						.index(indexName)
						.withJson(mappingInputStream)
						.build();
				elasticsearchClient.indices().create(createIndexRequest);
				log.info("인덱스 '{}'를 성공적으로 생성했습니다.", indexName);
				return ResponseEntity.ok(CustomApiResponse.success("인덱스 생성 성공"));
			}
		} catch (IOException e) {
			log.error("인덱스 '{}' 생성 중 오류 발생", indexName, e);
			return ResponseEntity.internalServerError()
					.body(CustomApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR, "인덱스 생성 실패: " + e.getMessage()));
		}
	}

	@PostMapping("/parkinglots-csv")
	public ResponseEntity<CustomApiResponse<?>> runParkingLotCsvToEsBatch() {
		try {
			log.info("ParkingLot CSV to ES 배치 작업 시작 요청됨");
			// The batch job now reads the file path from properties, so no parameter is needed.
			startBatchJobAsync(parkingLotCsvToEsJob, "parkingLotCsvToEsJob", null);
			return ResponseEntity.ok(
					CustomApiResponse.success("ParkingLot CSV to ES 배치 작업이 백그라운드에서 시작되었습니다.")
			);
		} catch (Exception e) {
			log.error("ParkingLot CSV to ES 배치 작업 시작 중 오류 발생", e);
			return ResponseEntity.internalServerError()
					.body(CustomApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR,
							"배치 작업 시작 중 오류가 발생했습니다: " + e.getMessage()));
		}
	}


	@PostMapping("/new-csv")
	public ResponseEntity<CustomApiResponse<?>> runCsvToEsBatchIncremental() {
		try {
			log.info("CSV to ES 증분 추가 배치 작업 시작 요청됨");
			startBatchJobAsync(placeCsvToEsIncrementalAddJob, "placeCsvToEsIncrementalAddJob", null);
			return ResponseEntity.ok(
				CustomApiResponse.success("CSV to ES 증분 추가 배치 작업이 백그라운드에서 시작되었습니다.")
			);
		} catch (Exception e) {
			log.error("CSV to ES 증분 추가 배치 작업 시작 중 오류 발생", e);
			return ResponseEntity.internalServerError()
				.body(CustomApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR,
					"배치 작업 시작 중 오류가 발생했습니다: " + e.getMessage()));
		}
	}

	@PostMapping("/all-csv")
	public ResponseEntity<CustomApiResponse<?>> runStagedCsvToEsBatchFullReload() {
		try {
			log.info("CSV to ES 단계적 전체 재구성 배치 작업 시작 요청됨");
			Resource[] resources = resourcePatternResolver.getResources("file:" + csvFilePath + "/*.csv");
			log.info("Resolved csvFilePath in controller: {}", csvFilePath);
			log.info("Number of resources found by controller: {}", resources.length);
			startStagedBatchJobAsync(placeCsvToEsJob, "placeCsvToEsJob", Arrays.asList(resources));
			return ResponseEntity.ok(
				CustomApiResponse.success("CSV to ES 단계적 전체 재구성 배치 작업이 백그라운드에서 시작되었습니다.")
			);
		} catch (IOException e) {
			log.error("CSV 파일 리소스를 찾는 중 오류 발생", e);
			return ResponseEntity.internalServerError()
				.body(CustomApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR,
					"CSV 파일 리소스를 찾는 중 오류가 발생했습니다: " + e.getMessage()));
		}
	}

	private void startStagedBatchJobAsync(Job jobToRun, String jobName, List<Resource> resources) {
		new Thread(() -> {
			int groupSize = 5;
			List<List<Resource>> resourceGroups = new ArrayList<>();
			for (int i = 0; i < resources.size(); i += groupSize) {
				resourceGroups.add(resources.subList(i, Math.min(i + groupSize, resources.size())));
			}

			for (int i = 0; i < resourceGroups.size(); i++) {
				List<Resource> group = resourceGroups.get(i);
				try {
					String fileResources = group.stream()
						.map(r -> {
							try {
								return r.getURL().toString();
							} catch (IOException e) {
								log.error("리소스 URL을 가져오는 데 실패했습니다.", e);
								return null;
							}
						})
						.filter(s -> s != null)
						.collect(Collectors.joining(","));

					log.info("단계적 배치 그룹 {}/{} 시작. 파일: {}", i + 1, resourceGroups.size(), fileResources);

					JobParametersBuilder builder = new JobParametersBuilder()
						.addString("JobID", String.valueOf(System.currentTimeMillis()));

					if (fileResources != null && !fileResources.isEmpty()) {
						builder.addString("fileResources", fileResources);
					}

					jobLauncher.run(jobToRun, builder.toJobParameters());
					log.info("단계적 배치 그룹 {}/{} 완료.", i + 1, resourceGroups.size());

					if (i < resourceGroups.size() - 1) {
						log.info("다음 그룹 실행 전 10초 대기...");
						Thread.sleep(10000);
					}

				} catch (Exception e) {
					log.error("단계적 배치 그룹 {}/{} 실행 중 오류 발생", i + 1, resourceGroups.size(), e);
					// 한 그룹이 실패해도 다음 그룹을 계속 시도
				}
			}
			log.info("모든 단계적 배치 그룹 실행 완료.");
		}).start();
	}

	private void startBatchJobAsync(Job jobToRun, String jobName, String fileResource) {
		new Thread(() -> {
			try {
				JobParametersBuilder builder = new JobParametersBuilder()
					.addString("JobID", String.valueOf(System.currentTimeMillis()));

				if (fileResource != null && !fileResource.isEmpty()) {
					// This parameter is now only for the place-related jobs
					builder.addString("fileResources", fileResource);
				}

				jobLauncher.run(jobToRun, builder.toJobParameters());
				log.info("{} 배치 작업 완료됨", jobName);
			} catch (Exception e) {
				log.error("{} 배치 작업 실행 중 오류 발생", jobName, e);
			}
		}).start();
	}
}
