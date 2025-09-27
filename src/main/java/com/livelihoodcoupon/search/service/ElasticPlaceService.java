package com.livelihoodcoupon.search.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.livelihoodcoupon.place.entity.Place;
import com.livelihoodcoupon.search.dto.SearchRequestDTO;
import com.livelihoodcoupon.search.dto.SearchToken;

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

@Service
public class ElasticPlaceService {

	private final ElasticsearchClient client;
	private final ElasticRestClient restClient;
	private final String index = "place";

	// 생성자를 통해 주입
	public ElasticPlaceService(ElasticsearchClient client, ElasticRestClient restClient) {
		this.client = client;
		this.restClient = restClient;
	}

	// 문서 저장
	public void savePlace(String id, Place place) throws IOException {
		client.index(i -> i
			.index(index)
			.id(id)
			.document(place)
		);
	}

	// 문서 조회
	public Place getPlace(String id) throws IOException {
		GetResponse<Place> response = client.get(g -> g
				.index(index)
				.id(id),
			Place.class
		);
		return response.found() ? response.source() : null;
	}

	// 검색 (예: category 필드가 '카페'인 문서)
	public List<Place> searchByCategory(String categoryKeyword) throws IOException {

		Query query = MatchQuery.of(m -> m
				.field("category")
				.query(categoryKeyword))
			._toQuery();
		SearchResponse<Place> response = client.search(s -> s
				.index(index)
				.query(query),
			Place.class
		);

		return response.hits().hits().stream()
			.map(Hit::source)
			.toList();
	}

	public SearchResponse<Place> searchPlace(List<SearchToken> resultList,
		SearchRequestDTO dto, int maxRecordSize, Pageable pageable) throws IOException {
		try {

			//쿼리생성
			List<Query> mustQueries = new ArrayList<>();

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
			mustQueries.add(geoQuery);

			for (SearchToken token : resultList) {
				switch (token.getFieldName()) {
					case "address":
						Query roadQuery = MatchQuery.of(m -> m
							.field("road_address")
							.query(token.getMorph())
						)._toQuery();
						mustQueries.add(roadQuery);
						break;
					case "category":
						Query categoryQuery = MatchQuery.of(m -> m
							.field("category")
							.query(token.getMorph())
						)._toQuery();
						mustQueries.add(categoryQuery);
						break;
					default:
						Query nameQuery = MatchQuery.of(m -> m
							.field("place_name")
							.query(token.getMorph())
						)._toQuery();
						mustQueries.add(nameQuery);
						break;
				}
			}

			Query finalQuery = BoolQuery.of(b -> b
				.must(mustQueries)
			)._toQuery();

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

			// 검색 요청
			SearchResponse<Place> response = client.search(s -> s
					.index(index)
					.query(finalQuery)
					.sort(geoSort)
					.from(pageFrom)
					.size(maxRecordSize),
				Place.class
			);

			return response;

		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}



/*

	// 검색 (예: category 필드가 '카페'인 문서)
	public Page<Place> searchDocuments(List<SearchToken> resultList, int maxRecordSize, Pageable pageable) throws
		IOException {

		// 1. Elasticsearch 클라이언트 생성
		//RestClient restClient = RestClient.builder(
		//	new HttpHost("localhost", 9200)
		//).build();

		//ElasticsearchClient client = new ElasticsearchClient(
		//	new RestClientTransport(restClient, new JacksonJsonpMapper())
		//);

		// 2. 쿼리 정의: 위치 + 이름
		Query geoQuery = GeoDistanceQuery.of(g -> g
			.field("location")
			.distance("1km")
			.location(loc -> loc
				.lat(37.572950)  // 종로구 중심점
				.lon(126.979357)
			)
		)._toQuery();

		Query nameQuery = MatchQuery.of(m -> m
			.field("place_name")
			.query("스타벅스")
		)._toQuery();

		Query categoryQuery = MatchQuery.of(m -> m
			.field("category")
			.query("카페")
		)._toQuery();

		// 3. Bool 쿼리로 결합
		Query finalQuery = BoolQuery.of(b -> b
			.must(geoQuery)
			.must(nameQuery)
			.must(categoryQuery)
		)._toQuery();

		// 4. 검색 요청
		SearchRequest searchRequest = SearchRequest.of(s -> s
			.index("cafes") // <<★ 인덱스 이름 주의
			.query(finalQuery)
		);

		SearchResponse<Object> response = client.search(searchRequest, Object.class);

		// 5. 결과 출력
		List<Hit<Object>> hits = response.hits().hits();
		for (Hit<Object> hit : hits) {
			System.out.println(hit.source());
		}

		client.close();

	}
*/

}
