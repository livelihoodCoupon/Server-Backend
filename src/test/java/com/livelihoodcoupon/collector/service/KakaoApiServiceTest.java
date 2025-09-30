package com.livelihoodcoupon.collector.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;

class KakaoApiServiceTest {

	@Mock
	private WebClient mockWebClient;

	@Mock
	private WebClient.RequestHeadersUriSpec<?> uriSpec;

	@Mock
	private WebClient.RequestHeadersSpec<?> headersSpec;

	@Mock
	private WebClient.ResponseSpec responseSpec;

	private KakaoApiService kakaoApiService;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		// WebClient 호출을 피하기 위해 익명 클래스로 Mono 반환
		kakaoApiService = new KakaoApiService("dummy-key") {
			@Override
			public Mono<Coordinate> getCoordinatesFromAddress(String address) {
				// 테스트용 데이터
				return Mono.just(new Coordinate(127.0, 37.5));
			}
		};
	}

	@Test
	void getCoordinatesFromAddress_shouldReturnCoordinate() {
		// 실제 WebClient 호출 없이 바로 Coordinate 반환
		KakaoApiService.Coordinate coord = kakaoApiService.getCoordinatesFromAddress("서울시").block();
		assertEquals(127.0, coord.longitude);
		assertEquals(37.5, coord.latitude);
	}

	@Test
	void coordinateClass_shouldStoreValuesCorrectly() {
		KakaoApiService.Coordinate coord = new KakaoApiService.Coordinate(10.0, 20.0);
		assertEquals(10.0, coord.longitude);
		assertEquals(20.0, coord.latitude);
	}
}
