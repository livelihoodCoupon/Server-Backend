package com.livelihoodcoupon.search.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.livelihoodcoupon.common.service.DictCacheService;
import com.livelihoodcoupon.search.dto.NoriToken;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.AnalyzeRequest;
import co.elastic.clients.elasticsearch.indices.AnalyzeResponse;
import co.elastic.clients.elasticsearch.indices.ElasticsearchIndicesClient;
import co.elastic.clients.elasticsearch.indices.analyze.AnalyzeToken;

//@SuppressWarnings("unchecked")
@DisplayName("SearchService 단위 테스트")
@ExtendWith(MockitoExtension.class)
class AnalyzerTestTest {
	@Mock
	ElasticsearchClient mockClient;
	@Mock
	DictCacheService mockDictCacheService;
	@InjectMocks
	AnalyzerTest analyzerTest;

	@BeforeEach
	void setup() {
		//mockClient = mock(ElasticsearchClient.class);
		//mockDictCacheService = mock(DictCacheService.class);
		//analyzerTest = new AnalyzerTest(mockClient, mockDictCacheService);
	}

	@Test
	@DisplayName("nori tokenizer 글자 분리테스트 성공")
	void testAnalyzeText() throws IOException {
		//given
		// 1. mock 생성
		ElasticsearchClient mockClient = mock(ElasticsearchClient.class);
		ElasticsearchIndicesClient mockIndices = mock(ElasticsearchIndicesClient.class);
		AnalyzeResponse mockResponse = mock(AnalyzeResponse.class);

		// 2. mock 관계 설정
		when(mockClient.indices()).thenReturn(mockIndices);
		when(mockIndices.analyze(any(AnalyzeRequest.class))).thenReturn(mockResponse);
		when(mockResponse.tokens()).thenReturn(List.of(
			AnalyzeToken.of(t -> t
				.token("종로")
				.startOffset(0)
				.endOffset(2)
				.position(0)
				.type("word")
			),
			AnalyzeToken.of(t -> t
				.token("참치")
				.startOffset(3)
				.endOffset(5)
				.position(1)
				.type("word")
			)
		));

		// 3. 테스트 대상 인스턴스 생성
		DictCacheService mockDictCacheService = mock(DictCacheService.class);
		when(mockDictCacheService.containsAddress("종로")).thenReturn(true);
		when(mockDictCacheService.containsCategory("참치")).thenReturn(false);

		//when
		AnalyzerTest analyzerTest = new AnalyzerTest(mockClient, mockDictCacheService);

		var result = analyzerTest.analyzeText("종로 참치");

		//then
		assertEquals(2, result.size());
		assertEquals("종로", result.get(0).getToken());
	}

	@Test
	@DisplayName("문장분리 구분하기 테스트 성공")
	void TestisCategoryAddress() throws IOException {
		//give
		String checkCategory = "place_name";

		//when
		var result = analyzerTest.isCategoryAddress("종로 참치");

		//then
		assertEquals(checkCategory, result);

	}

	@Test
	@DisplayName("해당위치의 주소찾기")
	void testgetAddress() {
		//given
		String address = "서울시 종로구 ";
		List<NoriToken> noriTokenList = new ArrayList<>();
		noriTokenList.add(new NoriToken("서울시", "address"));
		noriTokenList.add(new NoriToken("종로구", "address"));

		//when
		var result = analyzerTest.getAddress(noriTokenList);

		//then
		assertEquals(address, result);

	}
}