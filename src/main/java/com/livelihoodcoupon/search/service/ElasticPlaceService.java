package com.livelihoodcoupon.search.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.livelihoodcoupon.search.dto.AutocompleteDto;
import com.livelihoodcoupon.search.dto.AutocompleteResponseDto;
import com.livelihoodcoupon.search.dto.SearchRequestDto;
import com.livelihoodcoupon.search.dto.SearchToken;
import com.livelihoodcoupon.search.entity.PlaceDocument;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.DistanceUnit;
import co.elastic.clients.elasticsearch._types.GeoLocation;
import co.elastic.clients.elasticsearch._types.SortMode;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.GeoDistanceQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchBoolPrefixQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchPhrasePrefixQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MultiMatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
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

	/**
	 * 필드별 자동완성 호출, 호출후 합쳐서 내보내기
	 * @param dto
	 * @param maxSize
	 * @return
	 * @throws IOException
	 */
	public List<AutocompleteResponseDto> autocompletePlaceNames(AutocompleteDto dto, int maxSize)
		throws IOException {

		String keyword = dto.getWord();

		List<String> results = new ArrayList<>();
		results.addAll(searchField("road_address_sido.autocomplete", keyword, 8.0f, maxSize));
		results.addAll(searchField("road_address_sigungu.autocomplete", keyword, 7.0f, maxSize));
		results.addAll(searchField("road_address_dong.autocomplete", keyword, 6.0f, maxSize));
		results.addAll(searchField("road_address_road.autocomplete", keyword, 5.0f, maxSize));
		results.addAll(searchField("category_level1.autocomplete", keyword, 4.0f, maxSize));
		results.addAll(searchField("category_level2.autocomplete", keyword, 3.0f, maxSize));
		results.addAll(searchField("category_level3.autocomplete", keyword, 2.0f, maxSize));
		results.addAll(searchField("place_name.autocomplete", keyword, 1.0f, maxSize));

		log.info("======>autocompletePlaceNames___{}", keyword);

		// 중복 제거 및 정렬
		return results.stream()
			.distinct()
			.limit(maxSize)
			.map(AutocompleteResponseDto::new)
			.toList();
	}

	/**
	 * 자동완성 검색하기
	 * @param field
	 * @param keyword
	 * @param maxSize
	 * @return
	 * @throws IOException
	 */
	private List<String> searchField(String field,
		String keyword, float boost, int maxSize) throws IOException {

		Query query = MatchBoolPrefixQuery.of(m -> m
			.field(field)
			.query(keyword)
			.boost(boost)
		)._toQuery();

		log.info("======>searchField____{}__{}__{}", query, field, keyword);

		SearchResponse<PlaceDocument> response = client.search(s -> s
				.index(index)
				.query(query)
				.size(maxSize), // 필드당 최대 결과
			PlaceDocument.class
		);

		return response.hits().hits().stream()
			.map(hit -> getFieldValue(hit.source(), field))
			.filter(Objects::nonNull)
			.toList();
	}

	/**
	 * 자동완성 추출 필드 가져오기
	 * @param doc
	 * @param field
	 * @return
	 */
	private String getFieldValue(PlaceDocument doc, String field) {
		log.info("======>getFieldValue____{}", field);
		return switch (field) {
			case "road_address_sido.autocomplete" -> doc.getRoadAddressSido();
			case "road_address_sigungu.autocomplete" -> doc.getRoadAddressSigungu();
			case "road_address_dong.autocomplete" -> doc.getRoadAddressDong();
			case "road_address_road.autocomplete" -> doc.getRoadAddressRoad();
			case "category_level1.autocomplete" -> doc.getCategoryLevel1();
			case "category_level2.autocomplete" -> doc.getCategoryLevel2();
			case "category_level3.autocomplete" -> doc.getCategoryLevel3();
			case "place_name.autocomplete" -> doc.getPlaceName();
			default -> null;
		};
	}

	/**
	 * 엘라스틱서치 전체에서 검색
	 * 단어분리 : komoran 방식
	 * @param resultList
	 * @param dto
	 * @param pageable
	 * @return
	 * @throws IOException
	 */
	public SearchResponse<PlaceDocument> searchPlace(List<SearchToken> resultList,
		SearchRequestDto dto, Pageable pageable) throws IOException {

		try {
			//쿼리생성
			log.info("======>ElasticPlaceService 위도:{}, 경도:{}", dto.getLat(), dto.getLng());

			//위치 가져오기
			double refLat = (dto.getUserLat() != null) ? dto.getUserLat() : dto.getLat();
			double refLng = (dto.getUserLng() != null) ? dto.getUserLng() : dto.getLng();

			//위치 쿼리 생성
			Query geoQuery = GeoDistanceQuery.of(g -> g
				.field("location")
				.distance(String.valueOf(dto.getRadius()))
				.location(GeoLocation.of(loc -> loc
					.latlon(latlon -> latlon
						.lat(refLat)
						.lon(refLng)
					)
				))
			)._toQuery();

			//주소, 카테고리, 상가명 쿼리 생성
			List<Query> shouldQueries = new ArrayList<>();

			//목록이 공백일수 있기에 한번더체크
			List<SearchToken> resultList2;
			resultList2 = (resultList == null || resultList.isEmpty()) ? Collections.emptyList() : resultList;

			//분리된 단어로 쿼리 만들기
			int count = 0;
			for (SearchToken token : resultList2) {
				String fieldName = token.getFieldName(); //필드이름(address or category or place_name)
				String word = token.getMorph(); //분리된 단어

				if (fieldName != null) {
					count++;
					switch (fieldName) {
						case "address":
							//MatchPhrasePrefixQuery
							Query roadQuery = MatchPhrasePrefixQuery.of(
								m -> m.field("road_address").query(word.trim()).boost(1.0f))._toQuery();
							log.info("======>ElasticPlaceService fieldName road_address 검색2__{}", word);
							shouldQueries.add(roadQuery);
							break;
						case "category":
							Query categoryQuery = MultiMatchQuery.of(
								m -> m.fields("category.autocomplete^3", "category.nori^2", "category^1")
									.query(word.trim())
									.boost(2.0f))._toQuery();
							log.info("======>ElasticPlaceService fieldName category 검색__{}", word);
							shouldQueries.add(categoryQuery);
							break;
						default:
							Query nameQuery = MatchQuery.of(
								m -> m.field("place_name.autocomplete").query(word.trim()).boost(3.0f))._toQuery();
							log.info("======>ElasticPlaceService fieldName place_name 검색__{}", word);
							shouldQueries.add(nameQuery);
							break;
					}
				}
			}

/*
			//마지막쿼리 만들기
			int finalCount = count;
			Query finalQuery__ = BoolQuery.of(b -> b
				.must(geoQuery)
				.should(shouldQueries)
				.minimumShouldMatch(String.valueOf(finalCount)) // should 중 1개 이상만 일치하면 됨
			)._toQuery();
			log.info("======>ElasticPlaceService finalCount : {}", finalCount);
			log.info("====>ElasticPlaceService 333 finalQuery={}", finalQuery__);
*/

			//마지막 쿼리 만들기2
			int finalCount = count;
			BoolQuery boolQuery = BoolQuery.of(b -> {
				b.must(geoQuery);
				b.minimumShouldMatch(String.valueOf(finalCount)); // should 중 1개 이상만 일치하면 됨
				for (SearchToken token : resultList2) {

					// 1. edge_ngram 자동완성용 (BoolPrefix)
					b.should(MultiMatchQuery.of(m -> m
						.query(token.getMorph())
						.fields(List.of(
							"place_name.autocomplete^4",
							"road_address.autocomplete^3",
							"category.autocomplete^2"
						))
						.type(TextQueryType.BoolPrefix)
					)._toQuery());

					// 2. nori 형태소 분석기 기반 match 쿼리 (일반 텍스트 검색)
					b.should(MatchQuery.of(m -> m
						.field("place_name.nori")
						.query(token.getMorph())
						.operator(Operator.And)
						.boost(4.0f)
					)._toQuery());

					b.should(MatchQuery.of(m -> m
						.field("road_address.nori")
						.query(token.getMorph())
						.operator(Operator.And)
						.boost(3.0f)
					)._toQuery());

					b.should(MatchQuery.of(m -> m
						.field("category.nori")
						.query(token.getMorph())
						.operator(Operator.And)
						.boost(2.0f)
					)._toQuery());

				}
				return b;
			});
			Query finalQuery = boolQuery._toQuery();
			log.info("====>ElasticPlaceService 333 finalQuery={}", finalQuery);

			// 정렬 옵션
			SortOptions geoSort = SortOptions.of(s -> s
				.geoDistance(g -> g
					.field("location")
					.location(GeoLocation.of(loc -> loc
						.latlon(latlon -> latlon
							.lat(refLat)
							.lon(refLng)
						)
					))
					.order(SortOrder.Asc)
					.unit(DistanceUnit.Kilometers)
					.mode(SortMode.Min)
				)
			);

			int pageNumber = pageable.getPageNumber();         // 시작 인덱스
			int pageSize = pageable.getPageSize();         // 끝 인덱스
			int pageFrom = pageNumber * pageSize;         // 시작 인덱스

			// 검색 요청후  return
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
			throw new RuntimeException("Elasticsearch 검색에 실패하였습니다.", e);
		}
	}

}
