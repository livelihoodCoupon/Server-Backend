package com.livelihoodcoupon.search.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.test.util.ReflectionTestUtils;

import com.livelihoodcoupon.collector.service.KakaoApiService;
import com.livelihoodcoupon.search.dto.PlaceSearchResponseDto;
import com.livelihoodcoupon.search.dto.SearchRequestDto;
import com.livelihoodcoupon.search.dto.SearchToken;
import com.livelihoodcoupon.search.entity.GeoPoint;
import com.livelihoodcoupon.search.entity.PlaceDocument;

import co.elastic.clients.elasticsearch._types.ShardStatistics;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.elasticsearch.core.search.TotalHits;
import co.elastic.clients.elasticsearch.core.search.TotalHitsRelation;
import kr.co.shineware.nlp.komoran.core.Komoran;
import kr.co.shineware.nlp.komoran.model.KomoranResult;
import kr.co.shineware.nlp.komoran.model.Token;

@ExtendWith(MockitoExtension.class)
public class ElasticServiceTest {

	@Mock
	private ElasticPlaceService elasticPlaceService;

	@Mock
	private SearchService searchService;

	@Mock
	private KakaoApiService kakaoApiService;

	@Mock
	private AnalyzerTest analyzerTest;

	@Mock
	private Komoran komoran;

	@InjectMocks
	private ElasticService elasticService;

	@Test
	@DisplayName("엘라스틱 전체 검색 성공")
	void elasticSearch_shouldReturnPage() throws IOException {
		//given
		SearchRequestDto requestDto = new SearchRequestDto();
		requestDto.setQuery("서울시 카페");
		requestDto.setLat(37.5);
		requestDto.setLng(127.0);

		Token token1 = new Token("서울시", "NNP", 0, 2);
		Token token2 = new Token("카페", "NNG", 6, 8);

		// Mock Komoran analysis result
		List<SearchToken> mockTokens = List.of(
			new SearchToken("address", token1),
			new SearchToken("category", token2));
		//when(analyzerTest.isCategoryAddress("서울시")).thenReturn("address");
		//when(analyzerTest.isCategoryAddress("카페")).thenReturn(null);
		// searchPlace mock
		PlaceDocument doc = new PlaceDocument();
		doc.setPlaceName("카페1");
		doc.setLocation(new GeoPoint(37.51, 127.01));

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

		when(elasticPlaceService.searchPlace(any(), any(), any())).thenReturn(mockResponse);
		when(searchService.calculateDistance(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
			.thenReturn(1.23);
		KakaoApiService.Coordinate mockCoordinate = new KakaoApiService.Coordinate(37.5, 127.0);

		//when
		Page<PlaceSearchResponseDto> page = elasticService.elasticSearch(requestDto, 10, 100);

		//then
		assertThat(page.getContent())
			.hasSize(1)
			.extracting(PlaceSearchResponseDto::getPlaceName)
			.containsExactly("카페1");

		assertThat(page.getContent().get(0).getDistance()).isEqualTo(1.23);
	}

	@Test
	@DisplayName("검색한 위치해서 상가 위치까지 거리계산")
	void toSearchPositionDto_shouldReturnCorrectDto() {
		// given
		GeoPoint location = new GeoPoint(37.5, 127.0); // 위도/경도
		PlaceDocument doc = new PlaceDocument();
		doc.setLocation(location);
		doc.setPlaceName("테스트 카페");

		double refLat = 37.6;
		double refLng = 127.1;
		double mockDistance = 15.0;
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
	@DisplayName("단어 형태 분리 성공")
	void analysisChat_shouldReturnSearchTokenList() throws IOException, NoSuchFieldException, IllegalAccessException {
		// given
		String keyword = "서울시 강남구 카페";

		Token token1 = new Token("서울시", "NNP", 0, 2);
		Token token2 = new Token("강남구", "NNP", 3, 5);
		Token token3 = new Token("카페", "NNG", 6, 7);

		List<Token> tokenList = Arrays.asList(token1, token2, token3);

		KomoranResult mockResult = mock(KomoranResult.class);

		// analyzerTest 동작 정의
		when(analyzerTest.isCategoryAddress("서울시")).thenReturn("address");
		when(analyzerTest.isCategoryAddress("강남구")).thenReturn("address");
		when(analyzerTest.isCategoryAddress("카페")).thenReturn(null); // category는 null 처리

		// when
		List<SearchToken> result = elasticService.analysisChat(keyword);

		// then
		assertThat(result).hasSize(3);

		assertThat(result.get(0).getMorph()).isEqualTo("서울시");
		assertThat(result.get(0).getFieldName()).isEqualTo("address");

		assertThat(result.get(1).getMorph()).isEqualTo("강남구");
		assertThat(result.get(1).getFieldName()).isEqualTo("address");

		assertThat(result.get(2).getMorph()).isEqualTo("카페");
		assertThat(result.get(2).getFieldName()).isNull();

		// searchNewAddress 필드 검증
		String searchNewAddressValue = (String)ReflectionTestUtils.getField(elasticService, "searchNewAddress");
		assertThat(searchNewAddressValue).contains("서울시", "강남구");
	}

	//isAddress
	@Test
	@DisplayName("단어가 주소, 카테고리 여부 체크")
	void isAddress_isCategoryAddress_shouldReturnFalse() {
		ElasticService spyService = spy(elasticService);
	}

	@Test
	@DisplayName("단어분리후 주소, 카테고리 여부 체크")
	void isAddress_shouldReturnCategory() throws IOException {
		// given
		String morph = "서울시";
		String expectedCategory = "address";
		when(analyzerTest.isCategoryAddress(morph)).thenReturn(expectedCategory);

		// when
		String result = elasticService.isAddress(morph);

		// then
		assertThat(result).isEqualTo(expectedCategory);
		verify(analyzerTest).isCategoryAddress(morph); // 호출 확인
	}

	@Test
	@DisplayName("단어분리후 주소, 카테고리 null 반환")
	void isAddress_shouldReturnNullIfCategoryNotFound() throws IOException {
		// given
		String morph = "테스트";
		when(analyzerTest.isCategoryAddress(morph)).thenReturn(null);

		// when
		String result = elasticService.isAddress(morph);

		// then
		assertThat(result).isNull();
		verify(analyzerTest).isCategoryAddress(morph);
	}

}
