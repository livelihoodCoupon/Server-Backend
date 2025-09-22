package com.livelihoodcoupon.collector.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.livelihoodcoupon.collector.dto.Coord2RegionCodeResponse;
import com.livelihoodcoupon.collector.dto.KakaoResponse;
import com.livelihoodcoupon.common.exception.KakaoApiException;

import reactor.core.publisher.Mono;

/**
 * 카카오 API와의 통신을 담당하는 서비스 클래스
 *
 * <h3>주요 기능:</h3>
 * <ul>
 *   <li><b>장소 검색:</b> 키워드 기반으로 특정 반경 내 소비쿠폰 장소 검색</li>
 *   <li><b>좌표 변환:</b> 위도/경도 좌표를 행정구역 코드로 변환</li>
 *   <li><b>에러 처리:</b> API 호출 실패 시 적절한 예외 처리</li>
 * </ul>
 *
 * <h3>사용되는 카카오 API:</h3>
 * <ul>
 *   <li><code>/v2/local/search/keyword.json</code> - 키워드 기반 장소 검색</li>
 *   <li><code>/v2/local/geo/coord2regioncode.json</code> - 좌표를 행정구역 코드로 변환</li>
 * </ul>
 */
@Service
public class KakaoApiService {

	/** WebClient 인스턴스 - 카카오 API 호출용 */
	private final WebClient webClient;

	/**
	 * KakaoApiService 생성자
	 *
	 * <p>카카오 API 키를 주입받아 WebClient를 구성합니다.
	 * 기본 URL과 인증 헤더가 설정된 WebClient를 생성합니다.</p>
	 *
	 * @param apiKey 카카오 API 키 (application.yml에서 주입)
	 */
	public KakaoApiService(@Value("${kakao.api.key}") String apiKey) {
		this.webClient = WebClient.builder()
			.baseUrl("https://dapi.kakao.com")
			.defaultHeader("Authorization", "KakaoAK " + apiKey)
			.build();
	}

	/**
	 * 키워드 기반으로 장소를 검색합니다.
	 *
	 * <p>카카오 로컬 API를 사용하여 지정된 좌표와 반경 내에서 키워드에 해당하는 장소를 검색합니다.
	 * 페이지네이션을 지원하여 대량의 데이터도 효율적으로 처리할 수 있습니다.</p>
	 *
	 * @param keyword 검색할 키워드 (예: "소비쿠폰", "마트", "편의점")
	 * @param lng 경도 (X 좌표)
	 * @param lat 위도 (Y 좌표)
	 * @param radius 검색 반경 (미터 단위, 최대 20,000m)
	 * @param page 페이지 번호 (1부터 시작)
	 * @return 검색 결과 (KakaoResponse 객체)
	 * @throws KakaoApiException API 호출 실패 시
	 */
	public KakaoResponse searchPlaces(String keyword, double lng, double lat, int radius, int page) {
		return webClient.get()
			.uri(uriBuilder -> uriBuilder
				.path("/v2/local/search/keyword.json")
				.queryParam("query", keyword)
				.queryParam("x", lng)
				.queryParam("y", lat)
				.queryParam("radius", radius)
				.queryParam("page", page)
				.queryParam("size", 15) // 한 페이지당 최대 15개 결과
				.build())
			.retrieve()
			.onStatus(HttpStatusCode::isError, response -> response.bodyToMono(String.class)
				.flatMap(errorBody -> Mono.error(
					new KakaoApiException("Kakao API Error: " + response.statusCode() + " - " + errorBody,
						response.statusCode(), errorBody))))
			.bodyToMono(KakaoResponse.class)
			.block(); // 동기 호출 (비동기 처리 시 .subscribe() 사용)
	}

	/**
	 * 좌표를 행정구역 코드로 변환합니다.
	 *
	 * <p>카카오 좌표-행정구역 변환 API를 사용하여 위도/경도 좌표를
	 * 행정구역 코드(시도, 시군구, 읍면동)로 변환합니다.</p>
	 *
	 * @param lng 경도 (X 좌표)
	 * @param lat 위도 (Y 좌표)
	 * @return 행정구역 정보 (Coord2RegionCodeResponse 객체)
	 * @throws KakaoApiException API 호출 실패 시
	 */
	public Coord2RegionCodeResponse getRegionInfo(double lng, double lat) {
		return webClient.get()
			.uri(uriBuilder -> uriBuilder
				.path("/v2/local/geo/coord2regioncode.json")
				.queryParam("x", lng)
				.queryParam("y", lat)
				.build())
			.retrieve()
			.onStatus(HttpStatusCode::isError, response -> response.bodyToMono(String.class)
				.flatMap(errorBody -> Mono.error(
					new KakaoApiException("Kakao API Error: " + response.statusCode() + " - " + errorBody,
						response.statusCode(), errorBody))))
			.bodyToMono(Coord2RegionCodeResponse.class)
			.block(); // 동기 호출
	}

	public Mono<Coordinate> getCoordinatesFromAddress(String address) {
		return webClient.get()
			.uri(uriBuilder -> uriBuilder
				.path("/v2/local/search/address.json")
				.queryParam("query", address)
				.build())
			.retrieve()
			.bodyToMono(JsonNode.class)
			.handle((response, sink) -> {
				JsonNode documents = response.get("documents");
				if (documents != null && !documents.isEmpty()) {
					JsonNode location = documents.get(0);
					double x = location.get("x").asDouble(); // 경도 (longitude)
					double y = location.get("y").asDouble(); // 위도 (latitude)
					sink.next(new Coordinate(x, y));
				} else {
					sink.error(new RuntimeException("주소에 해당하는 좌표를 찾을 수 없습니다."));
				}
			});
	}

	public static class Coordinate {
		public final double longitude;
		public final double latitude;

		public Coordinate(double longitude, double latitude) {
			this.longitude = longitude;
			this.latitude = latitude;
		}
	}
}
