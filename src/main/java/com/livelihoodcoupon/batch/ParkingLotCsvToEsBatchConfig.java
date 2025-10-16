package com.livelihoodcoupon.batch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import com.livelihoodcoupon.common.dto.Coordinate;
import com.livelihoodcoupon.search.entity.ParkingLotDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.IOException;

@Slf4j
@Configuration
@RequiredArgsConstructor
@Profile("!test")
public class ParkingLotCsvToEsBatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager platformTransactionManager;
    private final ElasticsearchClient elasticsearchClient;
    private final ResourcePatternResolver resourcePatternResolver;

    private static final int CHUNK_SIZE = 1000;
    private static final String[] COLUMN_NAMES = new String[] {
        "id", "parkingLotNo", "parkingLotNm", "parkingLotSe", "parkingLotType",
        "roadAddress", "lotAddress", "parkingCapacity", "feedingSe", "enforceSe", "operDay",
        "weekOpenTime", "weekCloseTime", "satOpenTime", "satCloseTime", "holidayOpenTime",
        "holidayCloseTime", "parkingChargeInfo", "basicTime", "basicCharge", "addUnitTime",
        "addUnitCharge", "dayTicketApplyTime", "dayTicketCharge", "paymentMethod", 
        "specialComment", "institutionName", "phoneNumber", "lat", "lng",
        "disabledParkingZoneYn", "referenceDate", "institutionCode"
    };

    @Bean
    public Job parkingLotCsvToEsJob() {
        return new JobBuilder("parkingLotCsvToEsJob", jobRepository)
                .start(parkingLotCsvToEsStep())
                .build();
    }

    @Bean
    public Step parkingLotCsvToEsStep() {
        return new StepBuilder("parkingLotCsvToEsStep", jobRepository)
                .<ParkingLotCsvDto, ParkingLotDocument>chunk(CHUNK_SIZE, platformTransactionManager)
                .reader(parkingLotCsvToEsReader(null))
                .processor(parkingLotCsvToEsProcessor())
                .writer(parkingLotCsvToEsWriter())
                .faultTolerant()
                .skip(Exception.class)
                .skipLimit(Integer.MAX_VALUE)
                .listener(new com.livelihoodcoupon.batch.listener.CustomSkipListener())
                .build();
    }

    @Bean
    @StepScope
    public FlatFileItemReader<ParkingLotCsvDto> parkingLotCsvToEsReader(
            @Value("${batch.parking.csv.file}") String csvFilePath) {
        if (csvFilePath == null || csvFilePath.isBlank()) {
            throw new IllegalArgumentException("batch.parking.csv.file property is missing.");
        }

        Resource resource = resourcePatternResolver.getResource("file:" + csvFilePath);
        if (!resource.exists() || !resource.isReadable()) {
            throw new IllegalStateException("CSV file cannot be found or read: " + csvFilePath);
        }

        return new FlatFileItemReaderBuilder<ParkingLotCsvDto>()
                .name("parkingLotCsvToEsReader")
                .resource(resource)
                .linesToSkip(1)
                .encoding("UTF-8")
                .strict(true)
                .delimited()
                .names(COLUMN_NAMES)
                .targetType(ParkingLotCsvDto.class)
                .build();
    }

    @Bean
    public ItemProcessor<ParkingLotCsvDto, ParkingLotDocument> parkingLotCsvToEsProcessor() {
        return item -> {
            Double lat = toDoubleOrNull(item.getLat());
            Double lng = toDoubleOrNull(item.getLng());
            Coordinate location = (lat != null && lng != null) ? new Coordinate(lng, lat) : null;

            return ParkingLotDocument.builder()
                    .id(item.getId())
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
                    .location(location)
                    .disabledParkingZoneYn(item.getDisabledParkingZoneYn())
                    .referenceDate(item.getReferenceDate())
                    .institutionCode(item.getInstitutionCode())
                    .build();
        };
    }

    @Bean
    public ItemWriter<ParkingLotDocument> parkingLotCsvToEsWriter() {
        return chunk -> {
            if (chunk.getItems().isEmpty()) {
                return;
            }
            log.info("Writing {} items to Elasticsearch parkinglots index.", chunk.getItems().size());

            BulkRequest.Builder br = new BulkRequest.Builder();

            for (ParkingLotDocument doc : chunk.getItems()) {
                if (doc.getId() == null) continue; // Skip if ID is null
                br.operations(op -> op
                        .index(idx -> idx
                                .index("parkinglots")
                                .id(doc.getId().toString()) // Use the ID from CSV
                                .document(doc)
                        )
                );
            }

            try {
                co.elastic.clients.elasticsearch.core.BulkResponse response = elasticsearchClient.bulk(br.build());

                if (response.errors()) {
                    log.error("Elasticsearch bulk indexing for parkinglots had errors.");
                    response.items().forEach(item -> {
                        if (item.error() != null) {
                            log.error("Failed to index document ID {}: {}", item.id(), item.error().reason());
                        }
                    });
                }

            } catch (IOException e) {
                log.error("Elasticsearch bulk indexing for parkinglots failed with I/O exception.", e);
                throw e;
            }
        };
    }

    private static Double toDoubleOrNull(String v) {
        try {
            return (v == null || v.trim().isEmpty()) ? null : Double.parseDouble(v.trim());
        } catch (NumberFormatException e) {
            log.warn("Could not parse '{}' to Double.", v);
            return null;
        }
    }
}
