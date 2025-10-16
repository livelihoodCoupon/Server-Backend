package com.livelihoodcoupon.search.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.DistanceUnit;
import co.elastic.clients.elasticsearch._types.GeoLocation;
import co.elastic.clients.elasticsearch._types.SortMode;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.livelihoodcoupon.search.dto.AnalyzedAddress;
import com.livelihoodcoupon.search.dto.SearchRequestDto;
import com.livelihoodcoupon.search.dto.SearchToken;
import co.elastic.clients.elasticsearch.core.GetResponse;
import com.livelihoodcoupon.search.entity.ParkingLotDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ElasticParkingLotService {

    private final String index = "parkinglots";
    private final ElasticsearchClient client;

    public ParkingLotDocument getParkingLotById(String id) throws IOException {
        GetResponse<ParkingLotDocument> response = client.get(g -> g
                        .index(index)
                        .id(id),
                ParkingLotDocument.class
        );
        return response.found() ? response.source() : null;
    }

    public SearchResponse<ParkingLotDocument> searchParkingLot(AnalyzedAddress analyzedAddress, SearchRequestDto dto,
                                                               Pageable pageable, double searchCenterLat, double searchCenterLng,
                                                               double userSortLat, double userSortLng) throws IOException {
        try {
            log.info("======> ElasticParkingLotService searchParkingLot 위도:{}, 경도:{}", dto.getLat(), dto.getLng());

            List<SearchToken> tokens =
                    (analyzedAddress.getResultList() == null || analyzedAddress.getResultList().isEmpty())
                            ? Collections.emptyList() : analyzedAddress.getResultList();

            BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();

            if (!dto.isDisableGeoFilter()) {
                Query geoQuery = GeoDistanceQuery.of(g -> g
                        .field("location")
                        .distance(String.valueOf(dto.getRadius() + "km"))
                        .location(GeoLocation.of(loc -> loc.latlon(l -> l.lat(searchCenterLat).lon(searchCenterLng))))
                )._toQuery();
                boolQueryBuilder.filter(geoQuery);
            }

            List<Query> mustClauses = new ArrayList<>();
            List<Query> shouldClauses = new ArrayList<>();
            List<String> generalKeywords = new ArrayList<>();

            for (SearchToken token : tokens) {
                String fieldName = token.getFieldName();
                String word = token.getMorph();

                if (dto.isForceLocationSearch() && "address".equals(fieldName)) {
                    continue;
                }

                if ("address".equals(fieldName)) {
                    mustClauses.add(MatchQuery.of(m -> m.field("road_address.nori").query(word))._toQuery());
                } else {
                    generalKeywords.add(word);
                }
            }

            if (!generalKeywords.isEmpty()) {
                String generalQuery = String.join(" ", generalKeywords);
                shouldClauses.add(MultiMatchQuery.of(m -> m
                        .query(generalQuery)
                        .fields("parking_lot_nm.nori^3.0", "road_address.nori^1.5", "lot_address.nori^1.0")
                        .operator(Operator.And)
                )._toQuery());

                shouldClauses.add(MatchPhraseQuery.of(m -> m
                        .field("parking_lot_nm")
                        .query(dto.getQuery().trim())
                        .boost(5.0f)
                )._toQuery());
            }

            if (!mustClauses.isEmpty()) {
                boolQueryBuilder.must(mustClauses);
            }
            if (!shouldClauses.isEmpty()) {
                boolQueryBuilder.should(shouldClauses).minimumShouldMatch("1");
            }

            Query finalQuery = boolQueryBuilder.build()._toQuery();
            log.info("====> ElasticParkingLotService searchParkingLot finalQuery={}", finalQuery);

            List<SortOptions> sortOptions = new ArrayList<>();
            if ("accuracy".equals(dto.getSort())) {
                sortOptions.add(SortOptions.of(s -> s.score(score -> score.order(SortOrder.Desc))));
            } else { // "distance" or default
                sortOptions.add(SortOptions.of(s -> s.geoDistance(g -> g
                        .field("location")
                        .location(GeoLocation.of(loc -> loc
                                .latlon(latlon -> latlon
                                        .lat(userSortLat)
                                        .lon(userSortLng)
                                )
                        ))
                        .order(SortOrder.Asc)
                        .unit(DistanceUnit.Kilometers)
                        .mode(SortMode.Min)
                )));
                sortOptions.add(SortOptions.of(s -> s.score(score -> score.order(SortOrder.Desc))));
            }

            int pageNumber = pageable.getPageNumber();
            int pageSize = pageable.getPageSize();
            int pageFrom = pageNumber * pageSize;

            return client.search(s -> s
                            .index(index)
                            .query(finalQuery)
                            .sort(sortOptions)
                            .from(pageFrom)
                            .size(pageSize),
                    ParkingLotDocument.class
            );

        } catch (IOException e) {
            log.error("Elasticsearch parking lot 검색 중 오류 발생", e);
            throw new RuntimeException("Elasticsearch parking lot 검색에 실패하였습니다.", e);
        }
    }
}
