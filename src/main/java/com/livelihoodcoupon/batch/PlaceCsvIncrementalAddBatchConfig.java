package com.livelihoodcoupon.batch;

import java.io.IOException;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.exception.ConstraintViolationException;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JpaItemWriter;
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

import com.livelihoodcoupon.place.entity.Place;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@RequiredArgsConstructor
@Profile("!test")
public class PlaceCsvIncrementalAddBatchConfig {

	private final JobRepository jobRepository;
	private final PlatformTransactionManager platformTransactionManager;
	private final EntityManagerFactory entityManagerFactory;
	private final ResourcePatternResolver resourcePatternResolver;

	@Bean
	public Job placeCsvIncrementalAddJob() {
		return new JobBuilder("placeCsvIncrementalAddJob", jobRepository)
			.start(placeCsvIncrementalAddStep())
			.build();
	}

	@Bean
	public Step placeCsvIncrementalAddStep() {
		return new StepBuilder("placeCsvIncrementalAddStep", jobRepository)
			.<PlaceCsvDto, Place>chunk(1000, platformTransactionManager)
			.reader(incrementalMultiResourceItemReader(null))
			.processor(incrementalPlaceCsvProcessor())
			.writer(incrementalPlaceCsvWriter())
			.faultTolerant()
			.skip(ConstraintViolationException.class)
			.skipLimit(Integer.MAX_VALUE)
			.build();
	}

	private FlatFileItemReader<PlaceCsvDto> incrementalPlaceCsvReaderDelegate() {
		FlatFileItemReader<PlaceCsvDto> flatFileItemReader = new FlatFileItemReader<>();
		flatFileItemReader.setLinesToSkip(1);
		flatFileItemReader.setLineMapper(incrementalPlaceCsvLineMapper());
		return flatFileItemReader;
	}

	@Bean
	@StepScope
	public MultiResourceItemReader<PlaceCsvDto> incrementalMultiResourceItemReader(
		@Value("${batch.csv.incremental-path}") String csvFilePath) {
		MultiResourceItemReader<PlaceCsvDto> reader = new MultiResourceItemReader<>();
		try {
			Resource[] resources = resourcePatternResolver.getResources("file:" + csvFilePath + "/*.csv");
			reader.setResources(resources);
		} catch (IOException e) {
			log.error("증분 CSV 리소스 경로에서 파일을 로드하는 중 오류 발생: {}", csvFilePath, e);
			throw new RuntimeException("증분 CSV 리소스 로드 실패", e);
		}
		reader.setDelegate(incrementalPlaceCsvReaderDelegate());
		return reader;
	}

	@Bean
	public LineMapper<PlaceCsvDto> incrementalPlaceCsvLineMapper() {
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
	@StepScope
	public ItemProcessor<PlaceCsvDto, Place> incrementalPlaceCsvProcessor() {
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

			GeometryFactory geometryFactory = new GeometryFactory();
			WKTReader wktReader = new WKTReader(geometryFactory);
			Point point = null;
			try {
				String wktPoint = String.format("POINT (%s %s)", item.getLng(), item.getLat());
				point = (Point)wktReader.read(wktPoint);
				point.setSRID(4326);
			} catch (ParseException e) {
				log.error("placeId: {} 에 대한 WKT 파싱 오류 발생", item.getPlaceId(), e);
				return null;
			}

			return Place.builder()
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
				.location(point)
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
	public JpaItemWriter<Place> incrementalPlaceCsvWriter() {
		JpaItemWriter<Place> jpaItemWriter = new JpaItemWriter<>();
		jpaItemWriter.setEntityManagerFactory(entityManagerFactory);
		return jpaItemWriter;
	}
}
