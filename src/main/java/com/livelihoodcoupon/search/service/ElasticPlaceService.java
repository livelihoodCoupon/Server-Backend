package com.livelihoodcoupon.search.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.livelihoodcoupon.search.dto.NoriToken;
import com.livelihoodcoupon.search.dto.SearchRequestDto;
import com.livelihoodcoupon.search.entity.PlaceDocument;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.DistanceUnit;
import co.elastic.clients.elasticsearch._types.GeoLocation;
import co.elastic.clients.elasticsearch._types.SortMode;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.GeoDistanceQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ElasticPlaceService {

	private final String index = "places";
	private final ElasticsearchClient client;
	//private final ElasticsearchClient client;
	//private final ElasticRestClient restClient;

	public ElasticPlaceService(ElasticsearchClient client) {
		this.client = client;
	}

	// 문서 저장
	public void savePlace(String id, PlaceDocument place) throws IOException {
		client.index(i -> i
			.index(index)
			.id(id)
			.document(place)
		);
	}

	// 문서 조회
	public PlaceDocument getPlace(String id) throws IOException {
		GetResponse<PlaceDocument> response = client.get(g -> g
				.index(index)
				.id(id),
			PlaceDocument.class
		);
		return response.found() ? response.source() : null;
	}

	// 검색 (예: category 필드가 '카페'인 문서)
	public List<PlaceDocument> searchByCategory(String categoryKeyword) throws IOException {

		Query query = MatchQuery.of(m -> m
				.field("category")
				.query(categoryKeyword))
			._toQuery();
		SearchResponse<PlaceDocument> response = client.search(s -> s
				.index(index)
				.query(query),
			PlaceDocument.class
		);

		return response.hits().hits().stream()
			.map(Hit::source)
			.toList();
	}

	public SearchResponse<PlaceDocument> searchPlace(List<NoriToken> resultList,
		SearchRequestDto dto, int maxRecordSize, Pageable pageable) throws IOException {

		try {

			//쿼리생성
			List<Query> mustQueries = new ArrayList<>();

			log.info("======>ElasticPlaceService 위도:{}, 경도:{}", dto.getLat(), dto.getLng());

			Query geoQuery = GeoDistanceQuery.of(g -> g
				.field("location")
				.distance(String.valueOf(dto.getRadius()))
				.location(GeoLocation.of(loc -> loc
					.latlon(latlon -> latlon
						.lat(dto.getLat())
						.lon(dto.getLng())
					)
				))
			)._toQuery();
			//mustQueries.add(geoQuery);

			List<NoriToken> resultList2;
			resultList2 = (resultList == null || resultList.isEmpty()) ? Collections.emptyList() : resultList;
			int count = 0;
			for (NoriToken token : resultList2) {
				String fieldName = token.getFieldName();
				String word = token.getToken();

				if (fieldName != null) {
					count++;
					switch (fieldName) {
						case "load_address":
							/*서울시 검색안됨
							Query roadQuery = MatchQuery.of(m -> m
								.field("road_address")
								.query(token.getMorph())
							)._toQuery();*/
							/*
							Query roadQuery = MatchPhrasePrefixQuery.of(m -> m
								.field("road_address")
								.query(token.getMorph())
							)._toQuery();*/
							Query roadQuery = MatchQuery.of(m -> m
								.field("road_address")
								.query(word.trim())
							)._toQuery();

							log.info("======>ElasticPlaceService fieldName road_address 검색2__{}", word);
							mustQueries.add(roadQuery);
							break;
						case "category":
							Query categoryQuery = MatchQuery.of(m -> m
								.field("category")
								.query(word.trim())
							)._toQuery();
							log.info("======>ElasticPlaceService fieldName category 검색__{}", word);
							mustQueries.add(categoryQuery);
							break;
						default:
							//MatchPhrasePrefixQuery
							Query nameQuery = MatchQuery.of(m -> m
								.field("place_name")
								.query(word.trim())
							)._toQuery();

							log.info("======>ElasticPlaceService fieldName place_name 검색__{}", word);
							mustQueries.add(nameQuery);
							break;
					}
				}
			}

			List<Query> shouldQueries = new ArrayList<>();

			int finalCount = count;
			Query finalQuery = BoolQuery.of(b -> b
				.must(geoQuery)
				.must(mustQueries)
			)._toQuery();
			// .minimumShouldMatch(String.valueOf(finalCount)) // should 중 1개 이상만 일치하면 됨

			// 정렬 옵션
			SortOptions geoSort = SortOptions.of(s -> s
				.geoDistance(g -> g
					.field("location")
					.location(GeoLocation.of(loc -> loc
						.latlon(latlon -> latlon
							.lat(dto.getLat())
							.lon(dto.getLng())
						)
					))
					.order(SortOrder.Asc)
					.unit(DistanceUnit.Kilometers)
					.mode(SortMode.Min)
				)
			);
			int pageFrom = (int)pageable.getOffset();         // 시작 인덱스
			int pageSize = (int)pageable.getPageSize();         // 끝 인덱스

			log.info("====>ElasticPlaceService 333 finalQuery={}", finalQuery);
			//log.info("====>ElasticPlaceService 333 geoSort={}", geoSort);
			//log.info("====>ElasticPlaceService 333 pageFrom={}", pageFrom);
			//log.info("====>ElasticPlaceService 333 pageSize={}", pageSize);

			// 검색 요청
			SearchResponse<PlaceDocument> response = client.search(s -> s
					.index(index)
					.query(finalQuery)
					.sort(geoSort)
					.from(pageFrom)
					.size(pageSize),
				PlaceDocument.class
			);

			return response;

		} catch (IOException e) {
			e.printStackTrace();
			log.error("====>ElasticPlaceService error===={}", e);
			throw new RuntimeException("Elasticsearch 검색에 실패하였습니다.", e);
		}
	}

}
