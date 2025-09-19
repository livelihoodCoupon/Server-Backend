package com.livelihoodcoupon.batch;

import java.io.IOException;

import jakarta.persistence.EntityManagerFactory;

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
import com.livelihoodcoupon.place.repository.PlaceRepository;
import com.livelihoodcoupon.place.service.PlaceIdCacheService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@RequiredArgsConstructor
@Profile("!test")
public class PlaceCsvBatchConfig {

	private final JobRepository jobRepository;
	private final PlatformTransactionManager platformTransactionManager;
	private final EntityManagerFactory entityManagerFactory;
	private final ResourcePatternResolver resourcePatternResolver; // ResourcePatternResolver 주입
	private final PlaceRepository placeRepository; // 중복 확인을 위한 PlaceRepository 주입
	private final PlaceIdCacheService placeIdCacheService; // PlaceIdCacheService 주입

	@Bean
	public Job placeCsvJob() {
		return new JobBuilder("placeCsvJob", jobRepository)
			.start(placeCsvStep())
			.build();
	}

	@Bean
	public Step placeCsvStep() {
		return new StepBuilder("placeCsvStep", jobRepository)
			.<PlaceCsvDto, Place>chunk(1000, platformTransactionManager)
			.reader(multiResourceItemReader(null)) // 초기값은 null, 실제 값은 application.yml에서 로드
			.processor(placeCsvProcessor())
			.writer(placeCsvWriter())
			.build();
	}

	// MultiResourceItemReader를 위한 FlatFileItemReader 델리게이트
	private FlatFileItemReader<PlaceCsvDto> placeCsvReaderDelegate() {
		FlatFileItemReader<PlaceCsvDto> flatFileItemReader = new FlatFileItemReader<>();
		flatFileItemReader.setLinesToSkip(1); // 헤더 스킵
		flatFileItemReader.setLineMapper(placeCsvLineMapper());
		return flatFileItemReader;
	}

	@Bean
	@StepScope
	public MultiResourceItemReader<PlaceCsvDto> multiResourceItemReader(
		@Value("${batch.csv.file.path}") String csvFilePath) {
		MultiResourceItemReader<PlaceCsvDto> reader = new MultiResourceItemReader<>();
		try {
			Resource[] resources = resourcePatternResolver.getResources("file:" + csvFilePath + "/*.csv");
			reader.setResources(resources);
		} catch (IOException e) {
			log.error("CSV 리소스 경로에서 파일을 로드하는 중 오류 발생: {}", csvFilePath, e);
			throw new RuntimeException("CSV 리소스 로드 실패", e);
		}
		reader.setDelegate(placeCsvReaderDelegate());
		return reader;
	}

	@Bean
	public LineMapper<PlaceCsvDto> placeCsvLineMapper() {
		DefaultLineMapper<PlaceCsvDto> defaultLineMapper = new DefaultLineMapper<>();

		DelimitedLineTokenizer delimitedLineTokenizer = new DelimitedLineTokenizer();
		delimitedLineTokenizer.setNames("placeId", "region", "placeName", "roadAddress", "lotAddress", "lat", "lng",
			"phone", "categoryName", "keyword", "categoryGroupCode", "categoryGroupName", "placeUrl");
		delimitedLineTokenizer.setDelimiter(",");
		delimitedLineTokenizer.setStrict(false); // 컬럼 수 불일치 허용

		BeanWrapperFieldSetMapper<PlaceCsvDto> beanWrapperFieldSetMapper = new BeanWrapperFieldSetMapper<>();
		beanWrapperFieldSetMapper.setTargetType(PlaceCsvDto.class);

		defaultLineMapper.setLineTokenizer(delimitedLineTokenizer);
		defaultLineMapper.setFieldSetMapper(beanWrapperFieldSetMapper);

		return defaultLineMapper;
	}

	@Bean
	public ItemProcessor<PlaceCsvDto, Place> placeCsvProcessor() {
		return item -> {
			// 인메모리 캐시를 사용하여 placeId 중복 확인
			if (placeIdCacheService.contains(item.getPlaceId())) {
				log.debug("중복된 placeId (캐시에서): {} 건너뜀", item.getPlaceId());
				return null; // 캐시에 이미 존재하는 아이템은 건너뜀
			}
			// 새로운 아이템인 경우 캐시에 추가 (현재 배치 내 중복 처리용)
			placeIdCacheService.add(item.getPlaceId());
			GeometryFactory geometryFactory = new GeometryFactory();
			WKTReader wktReader = new WKTReader(geometryFactory);
			Point point = null;
			try {
				// CSV의 위도(lat)와 경도(lng)를 Point 객체로 변환
				// WKT 형식: POINT (경도 위도)
				String wktPoint = String.format("POINT (%s %s)", item.getLng(), item.getLat());
				point = (Point)wktReader.read(wktPoint);
				point.setSRID(4326); // SRID를 WGS84 (4326)으로 설정
			} catch (ParseException e) {
				log.error("placeId: {} 에 대한 WKT 파싱 오류 발생", item.getPlaceId(), e);
				return null; // 파싱 실패 시 해당 아이템 건너뜀
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
				.build();
		};
	}

	@Bean
	public JpaItemWriter<Place> placeCsvWriter() {
		JpaItemWriter<Place> jpaItemWriter = new JpaItemWriter<>();
		jpaItemWriter.setEntityManagerFactory(entityManagerFactory);
		return jpaItemWriter;
	}
}
