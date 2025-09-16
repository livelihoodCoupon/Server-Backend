package com.livelihoodcoupon.collector.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.livelihoodcoupon.collector.dto.Coord2RegionCodeResponse;
import com.livelihoodcoupon.collector.dto.KakaoResponse;

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
			.bodyToMono(Coord2RegionCodeResponse.class)
			.block();
	}
}
