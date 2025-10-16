package com.livelihoodcoupon.search.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ShardStatistics;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.elasticsearch.core.search.TotalHits;
import co.elastic.clients.elasticsearch.core.search.TotalHitsRelation;
import com.livelihoodcoupon.common.dto.Coordinate;
import com.livelihoodcoupon.common.exception.BusinessException;
import com.livelihoodcoupon.common.exception.ErrorCode;
import com.livelihoodcoupon.common.service.KakaoApiService;
import com.livelihoodcoupon.parkinglot.dto.NearbySearchRequest;
import com.livelihoodcoupon.parkinglot.dto.ParkingLotNearbyResponse;
import com.livelihoodcoupon.parkinglot.service.ParkingLotService;
import com.livelihoodcoupon.search.dto.*;
import com.livelihoodcoupon.search.entity.PlaceDocument;
import kr.co.shineware.nlp.komoran.model.Token;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("ElasticService 단위 테스트")
@ExtendWith(MockitoExtension.class)
class ElasticServiceTest {

	@Mock
	private ElasticPlaceService elasticPlaceService;
	@Mock
	private ElasticParkingLotService elasticParkingLotService;
	@Mock
	private SearchService searchService;
	@Mock
	private KakaoApiService kakaoApiService;
	@Mock
	private AnalyzerTest analyzerTest;
	@Mock
	private RedisService redisService;
	@Mock
	private ParkingLotService parkingLotService;

	@InjectMocks
	private ElasticService elasticService;


	@Test
	@DisplayName("지도검색 ServiceService 테스트 성공")
	void elasticSearch() throws IOException {
		// Given
		String query = "서울시 종로구 카페";

		SearchRequestDto req = new SearchRequestDto();
		req.setQuery(query);
		req.initDefaults();

		when(redisService.getWordInfo(anyString())).thenReturn("address");
		when(kakaoApiService.getCoordinatesFromAddress(anyString()))
			.thenReturn(Mono.just(new Coordinate(37.57104033689386, 127.0019782463416)));

		PlaceDocument doc = new PlaceDocument();
		doc.setPlaceName("카페1");
		doc.setLocation(new Coordinate(127.01, 37.51));

		Hit<PlaceDocument> hit = new Hit.Builder<PlaceDocument>()
			.source(doc)
			.index("places")
			.id("1")
			.build();
		TotalHits totalHits = new TotalHits.Builder().value(1).relation(TotalHitsRelation.Eq).build();
		HitsMetadata<PlaceDocument> hitsMetadata = new HitsMetadata.Builder<PlaceDocument>()
			.hits(List.of(hit))
			.total(totalHits)
			.build();
		SearchResponse<PlaceDocument> mockResponse = new SearchResponse.Builder<PlaceDocument>()
			.hits(hitsMetadata)
			.took(10L)
			.timedOut(false)
			.shards(new ShardStatistics.Builder().total(1).successful(1).failed(0).build())
			.build();

		when(elasticPlaceService.searchPlace(any(AnalyzedAddress.class), any(), any(), any(Double.class),
			any(Double.class), any(Double.class), any(Double.class))).thenReturn(
			mockResponse);
		when(searchService.calculateDistance(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
			.thenReturn(1.23);

		//when
		SearchServiceResult<PlaceSearchResponseDto> result = elasticService.elasticSearch(req, 10, 100);
		Page<PlaceSearchResponseDto> page = result.getPage();

		//then
		assertThat(page.getContent())
			.hasSize(1)
			.extracting(PlaceSearchResponseDto::getPlaceName)
			.containsExactly("카페1");
		assertThat(page.getContent().get(0).getDistance()).isEqualTo(1.23);
	}

	@Test
	@DisplayName("검색한 위치해서 상가 위치까지 거리계산")
	void toSearchPosition() {
		// given
		Coordinate location = new Coordinate(127.0, 37.5);
		PlaceDocument doc = new PlaceDocument();
		doc.setLocation(location);
		doc.setPlaceName("테스트 카페");

		double refLat = 37.6;
		double refLng = 127.1;
		double mockDistance = 1.0;
		when(searchService.calculateDistance(refLat, refLng, 37.5, 127.0)).thenReturn(mockDistance);

		// when
		PlaceSearchResponseDto result = elasticService.toSearchPosition(doc, refLat, refLng);

		// then
		assertThat(result).isNotNull();
		assertThat(result.getPlaceName()).isEqualTo("테스트 카페");
		assertThat(result.getDistance()).isEqualTo(mockDistance);

		verify(searchService, times(1)).calculateDistance(refLat, refLng, 37.5, 127.0);
	}

	@Test
	@DisplayName("주소로 위도, 경도 재탐색 성공")
	void handleAddressPosition_success() {
		// given
		String address = "서울 종로구";
		SearchRequestDto request = new SearchRequestDto();
		Coordinate coord = Coordinate.builder().lat(37.5).lng(127.0).build();

		when(kakaoApiService.getCoordinatesFromAddress(address)).thenReturn(Mono.just(coord));

		// when
		ResponseEntity<SearchRequestDto> response =
			elasticService.handleAddressPosition(address, request).block();

		// then
		assertEquals(37.5, response.getBody().getLat());
		assertEquals(127.0, response.getBody().getLng());
	}

	@Test
	@DisplayName("단어 형태 분리 테스트 성공")
	void testAnalysisChat_success() throws IOException {
		// Given
		String query = "서울시 강남구 카페";

		when(redisService.getWordInfo(anyString())).thenReturn("address");

		// When
		AnalyzedAddress analyzedAddress = elasticService.analysisChat(query);

		// Then
		assertNotNull(analyzedAddress);
		assertEquals(3, analyzedAddress.getResultList().size());
		assertTrue(analyzedAddress.getResultList().stream().anyMatch(token -> "address".equals(token.getFieldName())));
	}

	@Test
	@DisplayName("장소 기반 주변 주차장 검색 (쿼리 사용) 성공")
	void searchParkingLotsNearPlace_withQuery_success() throws IOException {
		// given
		// 자기 자신의 다른 메소드를 호출하는 경우, spy로 객체를 감싸서 특정 메소드의 행동만 정의한다.
		ElasticService spiedElasticService = spy(new ElasticService(elasticPlaceService, elasticParkingLotService, searchService, kakaoApiService, analyzerTest, redisService, parkingLotService));
		SearchRequestDto request = new SearchRequestDto();
		request.setQuery("강남역");
		request.setLat(null);
		request.setLng(null);

		double expectedLat = 37.4979;
		double expectedLng = 127.0276;
		SearchServiceResult<PlaceSearchResponseDto> mockPlaceSearchResult = new SearchServiceResult<>(Page.empty(), expectedLat, expectedLng);

		// elasticSearch 메소드 모의 처리
		doReturn(mockPlaceSearchResult).when(spiedElasticService).elasticSearch(any(SearchRequestDto.class), eq(1), eq(1));
		when(parkingLotService.findNearby(any(NearbySearchRequest.class))).thenReturn(new PageResponse<>(Page.empty(), 10, expectedLat, expectedLng));

		// when
		spiedElasticService.searchParkingLotsNearPlace(request);

		// then
		verify(spiedElasticService, times(1)).elasticSearch(request, 1, 1); // elasticSearch 호출 검증
		ArgumentCaptor<NearbySearchRequest> captor = ArgumentCaptor.forClass(NearbySearchRequest.class);
		verify(parkingLotService, times(1)).findNearby(captor.capture()); // findNearby 호출 검증
		assertThat(captor.getValue().getLat()).isEqualTo(expectedLat);
		assertThat(captor.getValue().getLng()).isEqualTo(expectedLng);
	}
}