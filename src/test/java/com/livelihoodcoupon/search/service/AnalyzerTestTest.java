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

@ExtendWith(MockitoExtension.class)
@DisplayName("SearchService 단위 테스트")
class AnalyzerTestTest {

	@Mock
	ElasticsearchClient mockClient;

	@Mock
	DictCacheService mockDictCacheService;

	@InjectMocks
	AnalyzerTest analyzerTest;

	@Mock
	private ElasticsearchIndicesClient indicesClient;

	@Mock
	private AnalyzeResponse analyzeResponse;

	@BeforeEach
	void setup() {
		analyzerTest = new AnalyzerTest(mockClient, mockDictCacheService);
	}

	@Test
	@DisplayName("nori tokenizer 글자 분리테스트 성공")
	void testAnalyzeText() throws IOException {

		String text = "종로 참치";

		when(mockDictCacheService.containsAddress("종로")).thenReturn(true);
		when(mockDictCacheService.containsCategory("참치")).thenReturn(false);

		// mock 연결
		when(mockClient.indices()).thenReturn(indicesClient);
		when(indicesClient.analyze(any(AnalyzeRequest.class))).thenReturn(analyzeResponse);
		List<AnalyzeToken> tokenList = List.of(
			AnalyzeToken.of(t -> t.token("종로").startOffset(0)
				.endOffset(2).position(1).type("")),
			AnalyzeToken.of(t -> t.token("참치").startOffset(3)
				.endOffset(5).position(2).type(""))
		);
		when(analyzeResponse.tokens()).thenReturn(tokenList);

		//when
		List<NoriToken> noriTokens = analyzerTest.analyzeText(text);

		//then
		assertEquals(2, noriTokens.size());
		assertEquals("종로", noriTokens.get(0).getToken());
	}

	@Test
	@DisplayName("분리된 단어 단어 address 여부 체크")
	void TestisCategoryAddress_check1() throws IOException {
		//give
		String checkCategory = "address";
		when(mockDictCacheService.containsAddress("서울시")).thenReturn(true);

		//when
		var result = analyzerTest.isCategoryAddress("서울시");

		//then
		assertEquals(checkCategory, result);
	}

	@Test
	@DisplayName("분리된 단어 단어 category 여부 체크")
	void TestisCategoryAddress_check2() throws IOException {
		//give
		String checkCategory = "category";
		when(mockDictCacheService.containsCategory("카페")).thenReturn(true);

		//when
		var result = analyzerTest.isCategoryAddress("카페");

		//then
		assertEquals(checkCategory, result);
	}

	@Test
	@DisplayName("분리된 단어 단어 place_name 여부 체크")
	void TestisCategoryAddress_check3() throws IOException {
		//give
		String checkCategory = "place_name";

		//when
		var result = analyzerTest.isCategoryAddress("박철헤어");

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