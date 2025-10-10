package com.livelihoodcoupon.batch;

import java.io.IOException;
import java.util.Arrays;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.LineMapper;
import org.springframework.batch.item.file.MultiResourceItemReader;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.transaction.PlatformTransactionManager;

import com.livelihoodcoupon.common.dto.Coordinate;
import com.livelihoodcoupon.search.entity.PlaceDocument;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@RequiredArgsConstructor
@Profile("!test")
public class PlaceCsvToEsBatchConfig {

	private final JobRepository jobRepository;
	private final PlatformTransactionManager platformTransactionManager;
	private final ResourcePatternResolver resourcePatternResolver;
	private final ElasticsearchClient elasticsearchClient; // ES 클라이언트

	@Bean
	public Job placeCsvToEsJob() {
		return new JobBuilder("placeCsvToEsJob", jobRepository)
			.start(placeCsvToEsStep())
			.build();
	}

	@Bean
	public Step placeCsvToEsStep() {
		return new StepBuilder("placeCsvToEsStep", jobRepository)
			.<PlaceCsvDto, PlaceDocument>chunk(1000, platformTransactionManager)
			.reader(esMultiResourceItemReader(null)) // 이름 변경된 Bean 호출
			.processor(placeCsvToEsProcessor())
			.writer(placeCsvToEsWriter())
			// 내결함성 추가: ES 색인 중 발생하는 모든 예외를 스킵 처리 (네트워크, 데이터 형식 등)
			.faultTolerant()
			.skip(Exception.class)
			.skipLimit(Integer.MAX_VALUE)
			.listener(new com.livelihoodcoupon.batch.listener.CustomSkipListener())
			.build();
	}

	// --- Reader 설정 (기존 PlaceCsvBatchConfig와 동일) ---
	private FlatFileItemReader<PlaceCsvDto> placeCsvReaderDelegate() {
		FlatFileItemReader<PlaceCsvDto> flatFileItemReader = new FlatFileItemReader<>();
		flatFileItemReader.setLinesToSkip(1);
		flatFileItemReader.setLineMapper(esPlaceCsvLineMapper()); // 이름 변경된 Bean 호출
		return flatFileItemReader;
	}

	@Bean
	@StepScope
	public MultiResourceItemReader<PlaceCsvDto> esMultiResourceItemReader(
		@Value("#{jobParameters['fileResources']}") String fileResources) {
		MultiResourceItemReader<PlaceCsvDto> reader = new MultiResourceItemReader<>();
		try {
			Resource[] resources = Arrays.stream(fileResources.split(","))
				.map(path -> {
					try {
						return new org.springframework.core.io.UrlResource(path);
					} catch (java.net.MalformedURLException e) {
						log.error("잘못된 파일 URL 형식입니다: {}", path, e);
						return null;
					}
				})
				.filter(r -> r != null)
				.toArray(Resource[]::new);

			reader.setResources(resources);
		} catch (Exception e) {
			log.error("잡 파라미터에서 리소스를 로드하는 중 오류 발생: {}", fileResources, e);
			throw new RuntimeException("리소스 로드 실패", e);
		}
		reader.setDelegate(placeCsvReaderDelegate());
		return reader;
	}

	@Bean
	public LineMapper<PlaceCsvDto> esPlaceCsvLineMapper() { // 메소드 이름 변경
		DefaultLineMapper<PlaceCsvDto> defaultLineMapper = new DefaultLineMapper<>();
		DelimitedLineTokenizer delimitedLineTokenizer = new DelimitedLineTokenizer();
		delimitedLineTokenizer.setNames("placeId", "region", "placeName", "roadAddress", "lotAddress", "lat", "lng",
			"phone", "categoryName", "keyword", "categoryGroupCode", "categoryGroupName", "placeUrl");
		delimitedLineTokenizer.setDelimiter(",");
		delimitedLineTokenizer.setStrict(false);
		BeanWrapperFieldSetMapper<PlaceCsvDto> beanWrapperFieldSetMapper = new BeanWrapperFieldSetMapper<>();
		beanWrapperFieldSetMapper.setTargetType(PlaceCsvDto.class);
		defaultLineMapper.setLineTokenizer(delimitedLineTokenizer);
		defaultLineMapper.setFieldSetMapper(beanWrapperFieldSetMapper);
		return defaultLineMapper;
	}

	// --- Processor 설정 ---
	@Bean
	public ItemProcessor<PlaceCsvDto, PlaceDocument> placeCsvToEsProcessor() {
		return item -> {
			// --- SQL 로직 기반 필드 분리 시작 ---
			String[] roadAddressParts =
				item.getRoadAddress() != null ? item.getRoadAddress().split(" ") : new String[0];
			String roadAddressSido = roadAddressParts.length > 0 ? roadAddressParts[0] : null;
			String roadAddressSigungu = roadAddressParts.length > 1 ? roadAddressParts[1] : null;
			String roadAddressRoad = roadAddressParts.length > 2 ? roadAddressParts[2] : null;

			String[] lotAddressParts = item.getLotAddress() != null ? item.getLotAddress().split(" ") : new String[0];
			String roadAddressDong = lotAddressParts.length > 2 ? lotAddressParts[2] : null;

			String[] categoryParts =
				item.getCategoryName() != null ? item.getCategoryName().split(" > ") : new String[0];
			String categoryLevel1 = categoryParts.length > 0 ? categoryParts[0].trim() : null;
			String categoryLevel2 = categoryParts.length > 1 ? categoryParts[1].trim() : null;
			String categoryLevel3 = categoryParts.length > 2 ? categoryParts[2].trim() : null;
			String categoryLevel4 = categoryParts.length > 3 ? categoryParts[3].trim() : null;
			// --- SQL 로직 기반 필드 분리 끝 ---

			// PlaceDocument 객체 생성
			return PlaceDocument.builder()
				.placeId(item.getPlaceId())
				.region(item.getRegion())
				.placeName(item.getPlaceName())
				.roadAddress(item.getRoadAddress())
				.lotAddress(item.getLotAddress())
				.phone(item.getPhone())
				.category(item.getCategoryName())
				.keyword(item.getKeyword())
				.categoryGroupCode(item.getCategoryGroupCode())
				.categoryGroupName(item.getCategoryGroupName())
				.placeUrl(item.getPlaceUrl())
				.location(new Coordinate(item.getLng(), item.getLat())) // ES에 맞는 좌표 객체 사용
				// 분리된 주소 및 카테고리 필드 추가
				.roadAddressSido(roadAddressSido)
				.roadAddressSigungu(roadAddressSigungu)
				.roadAddressRoad(roadAddressRoad)
				.roadAddressDong(roadAddressDong)
				.categoryLevel1(categoryLevel1)
				.categoryLevel2(categoryLevel2)
				.categoryLevel3(categoryLevel3)
				.categoryLevel4(categoryLevel4)
				.build();
		};
	}

	// --- Writer 설정 ---
	@Bean
	public ItemWriter<PlaceDocument> placeCsvToEsWriter() {
		return chunk -> {
			if (chunk.getItems().isEmpty()) {
				return;
			}
			log.info("Writing {} items to Elasticsearch.", chunk.getItems().size());

			BulkRequest.Builder br = new BulkRequest.Builder();

			for (PlaceDocument doc : chunk.getItems()) {
				br.operations(op -> op
					.index(idx -> idx
						.index("places")
						.id(doc.getPlaceId())
						.document(doc)
					)
				);
			}

			try {
				co.elastic.clients.elasticsearch.core.BulkResponse response = elasticsearchClient.bulk(br.build());

				if (response.errors()) {
					log.error("Elasticsearch bulk indexing had errors.");
					response.items().forEach(item -> {
						if (item.error() != null) {
							log.error("Failed to index document ID {}: {}", item.id(), item.error().reason());
						}
					});
					// 스텝을 실패 처리하려면 아래 주석을 해제하세요.
					// throw new RuntimeException("Bulk indexing failed for some items.");
				}

			} catch (IOException e) {
				log.error("Elasticsearch bulk indexing failed with I/O exception.", e);
				// 이 예외는 faultTolerant 스텝 설정에 의해 처리됩니다.
				throw e;
			}
		};
	}
}
