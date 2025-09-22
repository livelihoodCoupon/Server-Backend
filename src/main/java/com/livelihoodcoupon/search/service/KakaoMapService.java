package com.livelihoodcoupon.search.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;

import reactor.core.publisher.Mono;

@Service
public class KakaoMapService {

	private final WebClient webClient;

	public KakaoMapService(@Value("${kakao.api.key}") String apiKey) {
		this.webClient = WebClient.builder()
			.baseUrl("https://dapi.kakao.com")
			.defaultHeader(HttpHeaders.AUTHORIZATION, "KakaoAK " + apiKey)
			.build();
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
