package com.livelihoodcoupon.batch;

import java.io.IOException;

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
public class PlaceCsvToEsIncrementalAddBatchConfig {

	private final JobRepository jobRepository;
	private final PlatformTransactionManager platformTransactionManager;
	private final ResourcePatternResolver resourcePatternResolver;
	private final ElasticsearchClient elasticsearchClient;

	@Bean
	public Job placeCsvToEsIncrementalAddJob() {
		return new JobBuilder("placeCsvToEsIncrementalAddJob", jobRepository)
			.start(placeCsvToEsIncrementalAddStep())
			.build();
	}

	@Bean
	public Step placeCsvToEsIncrementalAddStep() {
		return new StepBuilder("placeCsvToEsIncrementalAddStep", jobRepository)
			.<PlaceCsvDto, PlaceDocument>chunk(1000, platformTransactionManager)
			.reader(esIncrementalMultiResourceItemReader(null))
			.processor(esIncrementalProcessor())
			.writer(esIncrementalWriter())
			.faultTolerant()
			.skip(Exception.class)
			.skipLimit(Integer.MAX_VALUE)
			.build();
	}

	private FlatFileItemReader<PlaceCsvDto> esIncrementalReaderDelegate() {
		FlatFileItemReader<PlaceCsvDto> flatFileItemReader = new FlatFileItemReader<>();
		flatFileItemReader.setLinesToSkip(1);
		flatFileItemReader.setLineMapper(esIncrementalLineMapper());
		return flatFileItemReader;
	}

	@Bean
	@StepScope
	public MultiResourceItemReader<PlaceCsvDto> esIncrementalMultiResourceItemReader(
		@Value("${batch.csv.incremental-path}") String csvFilePath) {
		MultiResourceItemReader<PlaceCsvDto> reader = new MultiResourceItemReader<>();
		try {
			Resource[] resources = resourcePatternResolver.getResources("file:" + csvFilePath + "/*.csv");
			reader.setResources(resources);
		} catch (IOException e) {
			log.error("증분 CSV 리소스 경로에서 파일을 로드하는 중 오류 발생: {}", csvFilePath, e);
			throw new RuntimeException("증분 CSV 리소스 로드 실패", e);
		}
		reader.setDelegate(esIncrementalReaderDelegate());
		return reader;
	}

	@Bean
	public LineMapper<PlaceCsvDto> esIncrementalLineMapper() {
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

	@Bean
	public ItemProcessor<PlaceCsvDto, PlaceDocument> esIncrementalProcessor() {
		return item -> {
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
				.location(new Coordinate(item.getLng(), item.getLat()))
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

	@Bean
	public ItemWriter<PlaceDocument> esIncrementalWriter() {
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
				elasticsearchClient.bulk(br.build());
			} catch (IOException e) {
				log.error("Elasticsearch bulk indexing failed.", e);
				throw new RuntimeException("Elasticsearch bulk indexing failed", e);
			}
		};
	}
}
