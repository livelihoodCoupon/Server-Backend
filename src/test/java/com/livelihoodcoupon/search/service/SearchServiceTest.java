package com.livelihoodcoupon.search.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import com.livelihoodcoupon.common.dto.Coordinate;
import com.livelihoodcoupon.common.service.KakaoApiService;
import com.livelihoodcoupon.place.entity.Place;
import com.livelihoodcoupon.search.dto.SearchRequestDto;
import com.livelihoodcoupon.search.dto.SearchResponseDto;
import com.livelihoodcoupon.search.dto.SearchToken;
import com.livelihoodcoupon.search.repository.SearchRepository;

import kr.co.shineware.nlp.komoran.model.Token;
import reactor.core.publisher.Mono;

@SuppressWarnings("unchecked")
@DisplayName("SearchService 단위 테스트")
@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

	@Mock
	private RedisService redisService;

	@Mock
	private SearchRepository searchRepository;

	@Mock
	private KakaoApiService kakaoApiService;

	@Mock
	private QueryService queryService;

	private SearchService searchService;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		searchService = new SearchService(redisService, searchRepository, kakaoApiService, queryService);
	}

	@Test
	@DisplayName("지도검색 ServiceService 테스트 성공")
	void testSearch() {
		// Given
		String query = "서울시 종로구 카페";

		SearchRequestDto req = new SearchRequestDto();
		req.setQuery(query);
		req.initDefaults();

		List<SearchToken> mockTokens = new ArrayList<>();

		SearchToken searchToken1 = new SearchToken("address",
			new Token("서울시", "NNP", 0, 3));
		mockTokens.add(searchToken1);

		SearchToken searchToken2 = new SearchToken("address",
			new Token("종로구", "NNP", 4, 6));
		mockTokens.add(searchToken2);

		SearchToken searchToken3 = new SearchToken("category",
			new Token("카페", "NNG", 8, 10));
		mockTokens.add(searchToken3);

		when(redisService.getWordInfo(anyString())).thenReturn("address");  // Mock Redis 서비스

		// KakaoMapService mock
		when(kakaoApiService.getCoordinatesFromAddress(anyString()))
			.thenReturn(Mono.just(new Coordinate(37.57104033689386, 127.0019782463416)));

		// Specification mocking
		Specification<Place> spec = mock(Specification.class);

		when(queryService.buildDynamicSpec(anyList(), any())).thenReturn(spec);
		List<Place> places = new ArrayList<>();
		Place place = Place.builder() // Use Place.builder()
			.placeId("testPlaceId")
			.placeName("Test Place")
			.roadAddress("Test Road Address")
			.lotAddress("Test Lot Address")
			.phone("123-456-7890")
			.category("Test Category")
			.keyword("Test Keyword")
			.categoryGroupCode("TC")
			.categoryGroupName("Test Category Group")
			.placeUrl("http://test.com")
			.location(new org.locationtech.jts.geom.GeometryFactory().createPoint(
				new org.locationtech.jts.geom.Coordinate(126.9863813979137,
					37.560949118173454))) // Initialize location directly
			.build();
		place.getLocation().setSRID(4326); // Set SRID

		places.add(place);

		Pageable pageable = PageRequest.of(req.getPage() - 1, 10, Sort.unsorted());
		Page<Place> pageResult = new PageImpl<>(places); // Changed from PlaceEntity
		when(searchRepository.findAll(spec, pageable)).thenReturn(pageResult);

		// When
		Page<SearchResponseDto> result = searchService.search(req, 10, 10);

		// Then
		assertNotNull(result);
		assertEquals(1, result.getContent().size());
		assertEquals(0, result.getPageable().getPageNumber());
		assertInstanceOf(SearchResponseDto.class, result.getContent().getFirst());
	}

	@Test
	@DisplayName("단어 형태 분리 테스트 성공")
	void testAnalysisChat() {
		// Given
		String query = "서울시 강남구 카페";

		when(redisService.getWordInfo(anyString())).thenReturn("address");

		// When
		List<SearchToken> tokens = searchService.analysisChat(query);

		// Then
		assertNotNull(tokens);
		assertEquals(3, tokens.size());
		assertTrue(tokens.stream().anyMatch(token -> "address".equals(token.getFieldName())));
	}

	@Test
	@DisplayName("단어 주소여부 테스트 성공")
	void testIsAddress() {
		// Given
		String morph = "서울시";
		String pos = "NNP";
		when(redisService.getWordInfo(morph)).thenReturn("address");

		// When
		String result = searchService.isAddress(morph, pos);

		// Then
		assertEquals("address", result);
	}
}
