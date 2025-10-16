package com.livelihoodcoupon.search.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.livelihoodcoupon.search.dto.AnalyzedAddress;
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
import co.elastic.clients.elasticsearch._types.query_dsl.MatchPhraseQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MultiMatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ElasticPlaceService {

	private static final List<String> ALLOWED_CATEGORIES = List.of(
		"음식", "음식점", "숙박", "카페", "편의점", "마트", "병원", "약국", "주차장", "주유소", "미용실", "안경"
	);
	private final String index = "places";
	private final ElasticsearchClient client;

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
	 **/
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
	 **/
	private List<String> searchField(String field,
		String keyword, float boost, int maxSize) throws IOException {

		Query query = MatchBoolPrefixQuery.of(m -> m
			.field(field)
			.query(keyword)
			.boost(boost)
		)._toQuery();

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
	 **/
	private String getFieldValue(PlaceDocument doc, String field) {
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
	 * @param analyzedAddress
	 * @param dto
	 * @param pageable
	 * @param searchCenterLat
	 * @param searchCenterLng
	 * @param userSortLat
	 * @param userSortLng
	 * @return
	 * @throws IOException
	 **/
	public SearchResponse<PlaceDocument> searchPlace(AnalyzedAddress analyzedAddress, SearchRequestDto dto,
		Pageable pageable, double searchCenterLat, double searchCenterLng, double userSortLat,
		double userSortLng) throws IOException {

		try {
			log.info("======>ElasticPlaceService searchPlace 위도:{}, 경도:{}", dto.getLat(), dto.getLng());

			List<SearchToken> tokens =
				(analyzedAddress.getResultList() == null || analyzedAddress.getResultList().isEmpty())
					? Collections.emptyList() : analyzedAddress.getResultList();

			// 1. 쿼리 빌더 초기화
			BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();

			// 2. 필수 조건: 위치 기반 필터링 (Geo-distance)
			if (!dto.isDisableGeoFilter()) {
				Query geoQuery = GeoDistanceQuery.of(g -> g
					.field("location")
					.distance(String.valueOf(dto.getRadius() + "km"))
					.location(GeoLocation.of(loc -> loc.latlon(l -> l.lat(searchCenterLat).lon(searchCenterLng))))
				)._toQuery();
				boolQueryBuilder.filter(geoQuery); // filter 절로 변경하여 스코어 계산에서 제외하고 캐싱 활용
			}

			// 3. 필수/선택 조건: 토큰 기반 쿼리 생성
			List<Query> mustClauses = new ArrayList<>();
			List<Query> shouldClauses = new ArrayList<>();
			List<String> generalKeywords = new ArrayList<>();

			for (SearchToken token : tokens) {
				String fieldName = token.getFieldName();
				String word = token.getMorph();

				// forceLocationSearch가 true이면, 검색어 내 지역 정보는 완전히 무시
				if (dto.isForceLocationSearch() && "address".equals(fieldName)) {
					continue; // 해당 지역 토큰은 쿼리 생성에서 제외
				}

				if ("address".equals(fieldName)) {
					mustClauses.add(MatchQuery.of(m -> m.field("road_address.nori").query(word))._toQuery());
				} else if ("category".equals(fieldName) && ALLOWED_CATEGORIES.contains(word)) {
					mustClauses.add(MatchQuery.of(m -> m.field("category.nori").query(word))._toQuery());
				} else {
					// 주소나 카테고리가 아닌 단어는 일반 키워드로 간주
					generalKeywords.add(word);
				}
			}

			// 일반 키워드가 있는 경우, should 절에 추가하여 관련도 점수에 반영
			if (!generalKeywords.isEmpty()) {
				String generalQuery = String.join(" ", generalKeywords);
				shouldClauses.add(MultiMatchQuery.of(m -> m
					.query(generalQuery)
					.fields("place_name.nori^3.0", "road_address.nori^1.5", "category.nori^1.0")
					.operator(Operator.And)
				)._toQuery());
				// 원본 검색어 전체에 대한 구문 일치(phrase match) 점수 추가
				shouldClauses.add(MatchPhraseQuery.of(m -> m
					.field("place_name")
					.query(dto.getQuery().trim())
					.boost(5.0f)
				)._toQuery());
			}

			// 생성된 must와 should 절을 bool 쿼리에 추가
			if (!mustClauses.isEmpty()) {
				boolQueryBuilder.must(mustClauses);
			}
			if (!shouldClauses.isEmpty()) {
				boolQueryBuilder.should(shouldClauses).minimumShouldMatch("1");
			}

			// 4. 최종 쿼리 생성
			Query finalQuery = boolQueryBuilder.build()._toQuery();
			log.info("====>ElasticPlaceService searchPlace finalQuery={}", finalQuery);

			// 5. 정렬 옵션 설정
			List<SortOptions> sortOptions = new ArrayList<>();
			if ("accuracy".equals(dto.getSort())) {
				sortOptions.add(SortOptions.of(s -> s.score(score -> score.order(SortOrder.Desc))));
			} else { // "distance" 또는 기본값
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
				// 거리순 정렬 시에도 관련도 점수를 2차 정렬 기준으로 추가
				sortOptions.add(SortOptions.of(s -> s.score(score -> score.order(SortOrder.Desc))));
			}

			// 6. 검색 실행 및 반환
			int pageNumber = pageable.getPageNumber();         // 시작 인덱스
			int pageSize = pageable.getPageSize();         // 끝 인덱스
			int pageFrom = pageNumber * pageSize;         // 시작 인덱스

			// 검색 요청후  return
			SearchResponse<PlaceDocument> response = client.search(s -> s
					.index(index)
					.query(finalQuery)
					.sort(sortOptions)
					.from(pageFrom)
					.size(pageSize),
				PlaceDocument.class
			);
			return response;

		} catch (IOException e) {
			log.error("Elasticsearch 검색 중 오류 발생", e);
			throw new RuntimeException("Elasticsearch 검색에 실패하였습니다.", e);
		}
	}

}
