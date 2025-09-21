package com.livelihoodcoupon.search.service;

import com.livelihoodcoupon.collector.repository.CollectorPlaceRepository;
import com.livelihoodcoupon.search.dto.SearchRequest;
import com.livelihoodcoupon.search.dto.SearchResponse;
import com.livelihoodcoupon.search.dto.SearchToken;
import com.livelihoodcoupon.collector.entity.PlaceEntity;
import com.livelihoodcoupon.search.repository.SearchRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import kr.co.shineware.nlp.komoran.model.Token;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
@DisplayName("SearchService 단위 테스트")
@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

	@Mock
	private RedisService redisService;

	@Mock
	private SearchRepository searchRepository;

	@Mock
	private KakaoMapService kakaoMapService;

	@Mock
	private QueryService queryService;

	private SearchService searchService;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		searchService = new SearchService(redisService, searchRepository,kakaoMapService, queryService);
	}

	@Test
	void testSearch() {
		// Given
		String query = "서울시 종로구 카페";

		SearchRequest req = new SearchRequest();
		req.setQuery(query);
		req.initDefaults();

		List<SearchToken> mockTokens = new ArrayList<>();

		SearchToken searchToken1 = new  SearchToken(new Token("서울시", "NNP", 0, 3));
		searchToken1.setFieldName("address");
		mockTokens.add(searchToken1);

		SearchToken searchToken2 = new SearchToken(new Token("종로구", "NNP", 4, 6));
		searchToken2.setFieldName("address");
		mockTokens.add(searchToken2);

		SearchToken searchToken3 = new SearchToken(new Token("카페", "NNG", 8, 10));
		searchToken3.setFieldName("category");
		mockTokens.add(searchToken3);

		when(redisService.getWordInfo(anyString())).thenReturn("address");  // Mock Redis 서비스

		// KakaoMapService mock
		when(kakaoMapService.getCoordinatesFromAddress(anyString()))
			.thenReturn(Mono.just(new KakaoMapService.Coordinate(37.57104033689386, 127.0019782463416)));


		// Specification mocking
		Specification<PlaceEntity> spec = mock(Specification.class);


		when(queryService.buildDynamicSpec(anyList(), any())).thenReturn(spec);
		List<PlaceEntity> placeEntities = new ArrayList<>();
		PlaceEntity place = new PlaceEntity();  // 가짜 PlaceEntity 객체 생성
		placeEntities.add(place);

		Pageable pageable = PageRequest.of(req.getPage()-1, 10, Sort.unsorted());
		Page<PlaceEntity> pageResult = new PageImpl<>(placeEntities);
		when(searchRepository.findAll(spec, pageable)).thenReturn(pageResult);

		// When
		Page<SearchResponse> result = searchService.search(req, 10, 10);

		System.out.println(result.getTotalElements()+"______");
		// Then
		assertNotNull(result);
		assertEquals(1, result.getContent().size());
		assertEquals(0, result.getPageable().getPageNumber());
		assertInstanceOf(SearchResponse.class, result.getContent().getFirst());
	}

	@Test
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
