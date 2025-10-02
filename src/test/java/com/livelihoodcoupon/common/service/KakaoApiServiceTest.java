package com.livelihoodcoupon.common.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.livelihoodcoupon.common.dto.Coord2RegionCodeResponse;
import com.livelihoodcoupon.common.dto.KakaoResponse;

import reactor.core.publisher.Mono;

public class KakaoApiServiceTest {

	String apiKey = "${kakao.api.key}";
	@Mock
	private WebClient mapWebClient;
	@Mock
	private WebClient naviWebClient;
	@InjectMocks
	private KakaoApiService service;

	@BeforeEach
	void setup() {
		service = new KakaoApiService(apiKey);

		// WebClient mock 생성
		mapWebClient = Mockito.mock(WebClient.class);

		// private final mapWebClient 필드에 mock 주입
		org.springframework.test.util.ReflectionTestUtils.setField(service, "mapWebClient", mapWebClient);
	}

	@Test
	@DisplayName("카카오API 접속 테스트")
	void KakaoApiService() {
		// given

		// when
		KakaoApiService service = new KakaoApiService(apiKey);

		// then
		WebClient mapClient = (WebClient)ReflectionTestUtils.getField(service, "mapWebClient");
		WebClient naviClient = (WebClient)ReflectionTestUtils.getField(service, "naviWebClient");

		assertThat(service).isNotNull();        // service 객체 존재 확인
		assertThat(mapClient).isNotNull();      // mapWebClient 존재 확인
		assertThat(naviClient).isNotNull();     // naviWebClient 존재 확인
	}

	@Test
	@DisplayName("키워드 기반으로 장소를 검색합니다.")
	void searchPlaces_shouldReturnMockResponse() {

		// mock 생성
		WebClient mapWebClient = Mockito.mock(WebClient.class);
		WebClient.RequestHeadersUriSpec uriSpec = Mockito.mock(WebClient.RequestHeadersUriSpec.class);
		WebClient.RequestHeadersSpec headersSpec = Mockito.mock(WebClient.RequestHeadersSpec.class);
		WebClient.ResponseSpec responseSpec = Mockito.mock(WebClient.ResponseSpec.class);

		// private 필드 주입
		ReflectionTestUtils.setField(service, "mapWebClient", mapWebClient);

		KakaoResponse mockResponse = new KakaoResponse();

		// 체인 Stubbing
		when(mapWebClient.get()).thenReturn(uriSpec);
		when(uriSpec.uri(any(Function.class))).thenReturn(headersSpec);
		when(headersSpec.retrieve()).thenReturn(responseSpec);
		when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
		when(responseSpec.bodyToMono(KakaoResponse.class)).thenReturn(Mono.just(mockResponse));

		// when
		KakaoResponse result = service.searchPlaces("테스트", 126.978, 37.566, 1000, 1);

		// then
		assertThat(result).isNotNull();
		assertThat(result).isEqualTo(mockResponse);
	}

	@Test
	@DisplayName("주소를 좌표로 변환합니다.")
	void getRegionInfo_shouldReturnMockResponse() {
		// given
		WebClient mapWebClient = Mockito.mock(WebClient.class);
		WebClient.RequestHeadersUriSpec uriSpec = Mockito.mock(WebClient.RequestHeadersUriSpec.class);
		WebClient.RequestHeadersSpec<?> headersSpec = Mockito.mock(WebClient.RequestHeadersSpec.class);
		WebClient.ResponseSpec responseSpec = Mockito.mock(WebClient.ResponseSpec.class);

		// KakaoApiService의 mapWebClient 교체
		ReflectionTestUtils.setField(service, "mapWebClient", mapWebClient);

		Coord2RegionCodeResponse mockResponse = new Coord2RegionCodeResponse();

		// 체인 stubbing
		when(mapWebClient.get()).thenReturn(uriSpec);
		when(uriSpec.uri(any(Function.class))).thenReturn(headersSpec);
		when(headersSpec.retrieve()).thenReturn(responseSpec);
		when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
		when(responseSpec.bodyToMono(Coord2RegionCodeResponse.class)).thenReturn(Mono.just(mockResponse));

		// when
		Coord2RegionCodeResponse result = service.getRegionInfo(126.978, 37.566);

		// then
		assertThat(result).isNotNull();
		assertThat(result).isEqualTo(mockResponse);
	}

	@Test
	@DisplayName("자동차 길찾기 API를 호출합니다.")
	void getDrivingRoute_shouldReturnMockJsonNode() {
		// given
		WebClient naviWebClient = Mockito.mock(WebClient.class);
		WebClient.RequestHeadersUriSpec uriSpec = Mockito.mock(WebClient.RequestHeadersUriSpec.class);
		WebClient.RequestHeadersSpec<?> headersSpec = Mockito.mock(WebClient.RequestHeadersSpec.class);
		WebClient.ResponseSpec responseSpec = Mockito.mock(WebClient.ResponseSpec.class);

		// KakaoApiService의 naviWebClient를 Mock으로 교체
		ReflectionTestUtils.setField(service, "naviWebClient", naviWebClient);

		// Mock JSON 응답 생성
		ObjectMapper mapper = new ObjectMapper();
		JsonNode mockResponse = mapper.createObjectNode().put("route", "mocked");

		// 체인 Stubbing
		when(naviWebClient.get()).thenReturn(uriSpec);
		when(uriSpec.uri(any(Function.class))).thenReturn(headersSpec);
		when(headersSpec.retrieve()).thenReturn(responseSpec);
		when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
		when(responseSpec.bodyToMono(JsonNode.class)).thenReturn(Mono.just(mockResponse));

		// when
		JsonNode result = service.getDrivingRoute(126.978, 37.566, 127.027, 37.497);

		// then
		assertThat(result).isNotNull();
		assertThat(result.get("route").asText()).isEqualTo("mocked");
	}

}
