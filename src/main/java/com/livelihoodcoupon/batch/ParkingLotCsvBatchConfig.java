package com.livelihoodcoupon.batch;

import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
import org.springframework.batch.item.file.FlatFileParseException;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;

import org.springframework.transaction.PlatformTransactionManager;

import com.livelihoodcoupon.parkinglot.entity.ParkingLot;

@Slf4j
@Configuration
@RequiredArgsConstructor
@Profile("!test")
public class ParkingLotCsvBatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;
    private final ResourcePatternResolver resourcePatternResolver;

    private static final int CHUNK_SIZE = 1000;

    private static final String[] COLUMN_NAMES = new String[] {
        "parkingLotNo", "parkingLotNm", "parkingLotSe", "parkingLotType",
        "roadAddress", "lotAddress", "parkingCapacity", "feedingSe", "enforceSe", "operDay",
        "weekOpenTime", "weekCloseTime", "satOpenTime", "satCloseTime", "holidayOpenTime",
        "holidayCloseTime", "parkingChargeInfo", "basicTime", "basicCharge", "addUnitTime",
        "addUnitCharge", "dayTicketApplyTime", "dayTicketCharge",
        "paymentMethod", "specialComment", "institutionName", "phoneNumber", "lat", "lng",
        "disabledParkingZoneYn", "referenceDate", "institutionCode"
    };


    @Bean
    public Job parkingLotCsvJob() {
        return new JobBuilder("parkingLotCsvJob", jobRepository)
            .start(parkingLotCsvStep())
            .build();
    }

    @Bean
    public Step parkingLotCsvStep() {
        return new StepBuilder("parkingLotCsvStep", jobRepository)
            .<ParkingLotCsvDto, ParkingLot>chunk(CHUNK_SIZE, transactionManager)
            .reader(parkingLotCsvReader(null))     // 파일 경로는 yml에서 주입
            .processor(parkingLotItemProcessor())  // 좌표 변환(JTS Point, SRID=4326)
            .writer(parkingLotItemWriter())        // JPA Writer
            // === 예외 내구성(필요한 최소만) ===
            .faultTolerant()
            .skip(FlatFileParseException.class) // 컬럼 깨짐 등 CSV 파싱 문제는 건너뜀
            .skipLimit(1000)                    // 과도한 실패 시엔 배치 중단
            .build();
    }

    /**
     * 단일 CSV만 읽는 리더.
     * application.yml: batch.parking.csv.file: data/csv/전국주차장정보표준데이터.csv
     */
    @Bean
    @StepScope
    public FlatFileItemReader<ParkingLotCsvDto> parkingLotCsvReader(
        @Value("${batch.parking.csv.file}") String csvFilePath
    ) {
        if (csvFilePath == null || csvFilePath.isBlank()) {
            // 설정 누락은 즉시 실패(문제 조기 발견)
            throw new IllegalArgumentException("batch.parking.csv.file 설정이 비어 있습니다.");
        }

        Resource resource = resourcePatternResolver.getResource("file:" + csvFilePath);
        if (!resource.exists() || !resource.isReadable()) {
            // 파일 경로/권한 문제는 빠르게 알 수 있게 예외
            throw new IllegalStateException("CSV 파일을 찾을 수 없거나 읽을 수 없습니다: " + csvFilePath);
        }

        return new FlatFileItemReaderBuilder<ParkingLotCsvDto>()
            .name("parkingLotCsvReader")
            .resource(resource)
            .linesToSkip(1)      // 헤더 스킵
            .encoding("UTF-8")
            .strict(true)        // 파일 검증은 엄격히(이미 existence 체크했지만 이중 안전장치)
            .delimited()
            .names(COLUMN_NAMES) // DTO 필드와 정확히 매핑되어야 함
            .targetType(ParkingLotCsvDto.class)
            .build();
    }

    @Bean
    public ItemProcessor<ParkingLotCsvDto, ParkingLot> parkingLotItemProcessor() {
        GeometryFactory geometryFactory = new GeometryFactory();
        WKTReader wktReader = new WKTReader(geometryFactory);

        return item -> {
            // "모두 넣기" 요구사항: 좌표 실패해도 레코드는 저장(= location만 null)
            // 단, 좌표 값이 존재하면 Point 변환을 시도하고 실패 시 해당 레코드만 location=null 처리

            Point point = null;
            String lat = trimOrNull(item.getLat());
            String lng = trimOrNull(item.getLng());

            if (lat != null && lng != null) {
                // 숫자 형식 검증(미리 경고 로그)
                Double latD = toDoubleOrNull(lat);
                Double lngD = toDoubleOrNull(lng);
                if (latD == null || lngD == null) {
                    log.warn("위/경도 숫자 변환 실패. parkingLotNo={}, lat='{}', lng='{}'. location을 null로 저장합니다.",
                        item.getParkingLotNo(), lat, lng);
                } else {
                    try {
                        // PlaceCsvBatchConfig 스타일: WKT로 안전 파싱
                        String wktPoint = String.format("POINT (%s %s)", lngD, latD); // (lng lat) 순서
                        point = (Point) wktReader.read(wktPoint);
                        point.setSRID(4326);
                    } catch (ParseException e) {
                        log.error("좌표 WKT 파싱 실패. parkingLotNo={}, lat={}, lng={}. location을 null로 저장합니다.",
                            item.getParkingLotNo(), latD, lngD, e);
                        point = null; // 저장은 계속 진행
                    }
                }
            }

            // CSV의 모든 필드를 가능한 한 그대로 매핑(누락/오류 있어도 스킵하지 않음)
            return ParkingLot.builder()
                .parkingLotNo(item.getParkingLotNo())
                .parkingLotNm(item.getParkingLotNm())
                .parkingLotSe(item.getParkingLotSe())
                .parkingLotType(item.getParkingLotType())
                .roadAddress(item.getRoadAddress())
                .lotAddress(item.getLotAddress())
                .parkingCapacity(item.getParkingCapacity())
                .feedingSe(item.getFeedingSe())
                .enforceSe(item.getEnforceSe())
                .operDay(item.getOperDay())
                .weekOpenTime(item.getWeekOpenTime())
                .weekCloseTime(item.getWeekCloseTime())
                .satOpenTime(item.getSatOpenTime())
                .satCloseTime(item.getSatCloseTime())
                .holidayOpenTime(item.getHolidayOpenTime())
                .holidayCloseTime(item.getHolidayCloseTime())
                .parkingChargeInfo(item.getParkingChargeInfo())
                .basicTime(item.getBasicTime())
                .basicCharge(item.getBasicCharge())
                .addUnitTime(item.getAddUnitTime())
                .addUnitCharge(item.getAddUnitCharge())
                .dayTicketApplyTime(item.getDayTicketApplyTime())
                .dayTicketCharge(item.getDayTicketCharge())
                .paymentMethod(item.getPaymentMethod())
                .specialComment(item.getSpecialComment())
                .institutionName(item.getInstitutionName())
                .phoneNumber(item.getPhoneNumber())
                .location(point)
                .disabledParkingZoneYn(item.getDisabledParkingZoneYn())
                .referenceDate(item.getReferenceDate())
                .institutionCode(item.getInstitutionCode())
                .build();
        };
    }

    @Bean
    public JpaItemWriter<ParkingLot> parkingLotItemWriter() {
        JpaItemWriter<ParkingLot> writer = new JpaItemWriter<>();
        writer.setEntityManagerFactory(entityManagerFactory);
        return writer;
    }

    // Helper
    private static String trimOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static Double toDoubleOrNull(String v) {
        try {
            return (v == null) ? null : Double.parseDouble(v);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
