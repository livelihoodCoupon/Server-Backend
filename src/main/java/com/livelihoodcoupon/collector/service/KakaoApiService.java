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

@Service
public class KakaoApiService {

	private final WebClient webClient;

	public KakaoApiService(@Value("${kakao.api.key}") String apiKey) {
		this.webClient = WebClient.builder()
			.baseUrl("https://dapi.kakao.com")
			.defaultHeader("Authorization", "KakaoAK " + apiKey)
			.build();
	}

	public KakaoResponse searchPlaces(String keyword, double lng, double lat, int radius, int page) {
		return webClient.get()
			.uri(uriBuilder -> uriBuilder
				.path("/v2/local/search/keyword.json")
				.queryParam("query", keyword)
				.queryParam("x", lng)
				.queryParam("y", lat)
				.queryParam("radius", radius)
				.queryParam("page", page)
				.queryParam("size", 15)
				.build())
			.retrieve()
			.onStatus(HttpStatusCode::isError, response -> response.bodyToMono(String.class)
				.flatMap(errorBody -> Mono.error(
					new KakaoApiException("Kakao API Error: " + response.statusCode() + " - " + errorBody,
						response.statusCode(), errorBody))))
			.bodyToMono(KakaoResponse.class)
			.block();
	}

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
			.block();
	}

	public Mono<Coordinate> getCoordinatesFromAddress(String address) {
		return webClient.get()
			.uri(uriBuilder -> uriBuilder
				.path("/v2/local/search/address.json")
				.queryParam("query", address)
				.build())
			.retrieve()
			.bodyToMono(JsonNode.class)
			.map(response -> {
				JsonNode documents = response.get("documents");
				if (documents != null && documents.size() > 0) {
					JsonNode location = documents.get(0);
					double x = location.get("x").asDouble(); // 경도 (longitude)
					double y = location.get("y").asDouble(); // 위도 (latitude)
					return new Coordinate(x, y);
				} else {
					throw new RuntimeException("주소에 해당하는 좌표를 찾을 수 없습니다.");
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
