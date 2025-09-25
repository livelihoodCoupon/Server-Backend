package com.livelihoodcoupon.route.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.livelihoodcoupon.common.service.KakaoApiService;
import com.livelihoodcoupon.route.dto.Coordinate;
import com.livelihoodcoupon.route.dto.KakaoRouteResponse;
import com.livelihoodcoupon.route.dto.RouteRequest;
import com.livelihoodcoupon.route.dto.RouteResponse;
import com.livelihoodcoupon.route.dto.RouteStep;
import com.livelihoodcoupon.route.dto.RouteType;

/**
 * 카카오 길찾기 API 어댑터
 * 통합된 KakaoApiService를 사용하여 자동차 경로를 제공합니다.
 */
@Slf4j
@Service
public class KakaoRouteProvider implements RouteProvider {

	private final KakaoApiService kakaoApiService;
	private final ObjectMapper objectMapper;

	public KakaoRouteProvider(KakaoApiService kakaoApiService, ObjectMapper objectMapper) {
		this.kakaoApiService = kakaoApiService;
		this.objectMapper = objectMapper;
	}

	@Override
	public RouteResponse getRoute(RouteRequest request) {
		log.info("카카오 API 경로 조회 시작: {} -> {}, 타입: {}", 
			request.getStartLng(), request.getStartLat(), request.getRouteType());
		
		JsonNode jsonResponse = kakaoApiService.getDrivingRoute(
			request.getStartLng(),
			request.getStartLat(),
			request.getEndLng(),
			request.getEndLat()
		);

		log.debug("카카오 API 응답: {}", jsonResponse);

		try {
			KakaoRouteResponse kakaoResponse = objectMapper.treeToValue(jsonResponse, KakaoRouteResponse.class);
			return convertToRouteResponse(kakaoResponse, request.getRouteType());
		} catch (Exception e) {
			log.error("카카오 API 응답 변환 실패", e);
			throw new RuntimeException("카카오 API 응답 변환 실패", e);
		}
	}

	@Override
	public boolean supports(RouteType routeType) {
		// 카카오내비 API는 자동차 경로만 지원
		return routeType == RouteType.DRIVING;
	}

	@Override
	public String getProviderName() {
		return "KakaoNavi";
	}

	/**
	 * 카카오내비 API 응답을 RouteResponse로 변환합니다.
	 */
	private RouteResponse convertToRouteResponse(KakaoRouteResponse kakaoResponse, RouteType routeType) {
		if (kakaoResponse.getRoutes() == null || kakaoResponse.getRoutes().isEmpty()) {
			return RouteResponse.builder()
				.coordinates(new ArrayList<>())
				.totalDistance(0)
				.totalDuration(0)
				.routeType(routeType)
				.steps(new ArrayList<>())
				.build();
		}

		KakaoRouteResponse.Route route = kakaoResponse.getRoutes().get(0);
		List<Coordinate> coordinates = new ArrayList<>();
		List<RouteStep> steps = new ArrayList<>();

		// Summary에서 전체 거리와 시간 정보 가져오기
		int totalDistance = route.getSummary() != null ? route.getSummary().getDistance() : 0;
		int totalDuration = route.getSummary() != null ? route.getSummary().getDuration() : 0;

		// Sections에서 상세 경로 정보 처리
		if (route.getSections() != null) {
			for (KakaoRouteResponse.Route.Section section : route.getSections()) {
				// Roads에서 좌표 정보 추출
				if (section.getRoads() != null) {
					for (KakaoRouteResponse.Route.Section.Road road : section.getRoads()) {
						if (road.getVertexes() != null && road.getVertexes().size() >= 2) {
							// vertexes는 [x1, y1, x2, y2, ...] 형태의 1차원 배열
							for (int i = 0; i < road.getVertexes().size(); i += 2) {
								if (i + 1 < road.getVertexes().size()) {
									coordinates.add(Coordinate.builder()
										.lng(road.getVertexes().get(i))
										.lat(road.getVertexes().get(i + 1))
										.build());
								}
							}
						}
					}
				}

				// Guides에서 안내 정보 추출
				if (section.getGuides() != null) {
					for (KakaoRouteResponse.Route.Section.Guide guide : section.getGuides()) {
						steps.add(RouteStep.builder()
							.instruction(guide.getGuidance())
							.distance(guide.getDistance())
							.duration(guide.getDuration())
							.startLocation(Coordinate.builder()
								.lng(guide.getX())
								.lat(guide.getY())
								.build())
							.endLocation(Coordinate.builder()
								.lng(guide.getX())
								.lat(guide.getY())
								.build())
							.build());
					}
				}
			}
		}

		return RouteResponse.builder()
			.coordinates(coordinates)
			.totalDistance(totalDistance)
			.totalDuration(totalDuration)
			.routeType(routeType)
			.steps(steps)
			.build();
	}
}
