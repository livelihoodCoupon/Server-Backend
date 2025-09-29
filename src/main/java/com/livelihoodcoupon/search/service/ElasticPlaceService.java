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
import co.elastic.clients.elasticsearch._types.query_dsl.MatchPhrasePrefixQuery;
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
		results.addAll(searchField("place_name.autocomplete", keyword, maxSize));
		results.addAll(searchField("category_level1.autocomplete", keyword, maxSize));
		results.addAll(searchField("category_level2.autocomplete", keyword, maxSize));
		results.addAll(searchField("category_level3.autocomplete", keyword, maxSize));
		results.addAll(searchField("road_address_sido.autocomplete", keyword, maxSize));
		results.addAll(searchField("road_address_sigugun.autocomplete", keyword, maxSize));
		results.addAll(searchField("road_address_dong.autocomplete", keyword, maxSize));
		results.addAll(searchField("road_address_road.autocomplete", keyword, maxSize));

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
	private List<String> searchField(String field, String keyword, int maxSize) throws IOException {
		Query query = MatchPhrasePrefixQuery.of(m -> m
			.field(field)
			.query(keyword)
		)._toQuery();

		log.info("======>searchField____{}", query);

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
			case "place_name.autocomplete" -> doc.getPlaceName();
			case "category_level1.autocomplete" -> doc.getCategoryLevel1();
			case "category_level2.autocomplete" -> doc.getCategoryLevel2();
			case "category_level3.autocomplete" -> doc.getCategoryLevel3();
			case "road_address_sido.autocomplete" -> doc.getRoadAddressSido();
			case "road_address_sigugun.autocomplete" -> doc.getRoadAddressSigungu();
			case "road_address_dong.autocomplete" -> doc.getRoadAddressDong();
			case "road_address_road.autocomplete" -> doc.getRoadAddressRoad();
			default -> null;
		};
	}

	/**
	 * 엘라스틱서치 전체에서 검색
	 * 단어분리 : komoran 방식
	 * @param resultList
	 * @param dto
	 * @param maxRecordSize
	 * @param pageable
	 * @return
	 * @throws IOException
	 */
	public SearchResponse<PlaceDocument> searchPlace(List<SearchToken> resultList,
		SearchRequestDto dto, int maxRecordSize, Pageable pageable) throws IOException {

		try {
			//쿼리생성
			//log.info("======>ElasticPlaceService 위도:{}, 경도:{}", dto.getLat(), dto.getLng());

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
			List<Query> mustQueries = new ArrayList<>();
			List<Query> mustQueriesAddress = new ArrayList<>();
			List<Query> mustQueriesCategory = new ArrayList<>();
			List<Query> mustQueriesShopName = new ArrayList<>();

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
							Query categoryQuery = MatchQuery.of(
								m -> m.field("category").query(word.trim()).boost(2.0f))._toQuery();
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

			//마지막쿼리 만들기
			int finalCount = count;
			Query finalQuery = BoolQuery.of(b -> b
				.must(geoQuery)
				.should(shouldQueries)
				.minimumShouldMatch(String.valueOf(finalCount)) // should 중 1개 이상만 일치하면 됨
			)._toQuery();
			log.info("======>ElasticPlaceService finalCount : {}", finalCount);

/*			List<Query> shouldQueries = new ArrayList<>();
			shouldQueries.add(MatchQuery.of(
				m -> m.field("road_address.autocomplete").query(dto.getQuery()))._toQuery());
			//shouldQueries.add(MatchQuery.of(
			//	m -> m.field("lot_address.autocomplete").query(dto.getQuery()))._toQuery());
			shouldQueries.add(MatchQuery.of(
				m -> m.field("category.autocomplete").query(dto.getQuery()))._toQuery());
			shouldQueries.add(MatchQuery.of(
				m -> m.field("place_name.autocomplete").query(dto.getQuery()).boost(2.0f))._toQuery());
			Query finalQuery = BoolQuery.of(b -> b
				.must(geoQuery)
				.should(shouldQueries)
				.minimumShouldMatch(String.valueOf(1)) // should 중 1개 이상만 일치하면 됨
			)._toQuery(); */

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

			log.info("====>ElasticPlaceService 333 finalQuery={}", finalQuery);

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
			log.error("====>ElasticPlaceService error===={}", e);
			throw new RuntimeException("Elasticsearch 검색에 실패하였습니다.", e);
		}
	}

}
