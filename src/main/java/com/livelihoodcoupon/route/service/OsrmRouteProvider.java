package com.livelihoodcoupon.route.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.livelihoodcoupon.common.dto.Coordinate;
import com.livelihoodcoupon.route.dto.RouteRequest;
import com.livelihoodcoupon.route.dto.RouteResponse;
import com.livelihoodcoupon.route.dto.RouteStep;
import com.livelihoodcoupon.route.dto.RouteType;
import com.livelihoodcoupon.route.util.PolylineDecoder;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * OSRM (Open Source Routing Machine) 기반 경로 제공자
 * 도보, 자전거, 대중교통 경로를 지원합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OsrmRouteProvider implements RouteProvider {

	private final WebClient webClient;

	@Value("${osrm.servers.car:http://localhost:5004}")
	private String carServerUrl;

	@Value("${osrm.servers.foot:http://localhost:5001}")
	private String footServerUrl;

	@Value("${osrm.servers.bike:http://localhost:5002}")
	private String bikeServerUrl;

	@Value("${osrm.servers.transit:http://localhost:5003}")
	private String transitServerUrl;

	@Override
	public RouteResponse getRoute(RouteRequest request) {
		try {
			log.info("OSRM 경로 조회 시작: {} -> {}, 타입: {}",
				request.getStartLng(), request.getStartLat(), request.getRouteType());

			String profile = getOsrmProfile(request.getRouteType());
			String serverUrl = getServerUrl(request.getRouteType());
			String url = String.format("%s/route/v1/%s/%f,%f;%f,%f?overview=full&steps=true",
				serverUrl, profile,
				request.getStartLng(), request.getStartLat(),
				request.getEndLng(), request.getEndLat());

			log.debug("OSRM API 호출 URL: {}", url);

			OsrmResponse response = webClient.get()
				.uri(url)
				.retrieve()
				.bodyToMono(OsrmResponse.class)
				.block();

			if (response == null || response.getRoutes() == null || response.getRoutes().isEmpty()) {
				throw new RuntimeException("OSRM에서 경로를 찾을 수 없습니다");
			}

			return convertToRouteResponse(response, request.getRouteType());

		} catch (WebClientResponseException e) {
			log.error("OSRM API 호출 실패: {}", e.getMessage());
			throw new RuntimeException("OSRM 서비스 호출에 실패했습니다: " + e.getMessage());
		} catch (Exception e) {
			log.error("OSRM 경로 조회 중 오류 발생", e);
			throw new RuntimeException("경로 조회 중 오류가 발생했습니다: " + e.getMessage());
		}
	}

	@Override
	public boolean supports(RouteType routeType) {
		return routeType == RouteType.DRIVING
			|| routeType == RouteType.WALKING
			|| routeType == RouteType.CYCLING
			|| routeType == RouteType.TRANSIT;
	}

	@Override
	public String getProviderName() {
		return "OSRM";
	}

	private String getOsrmProfile(RouteType routeType) {
		return switch (routeType) {
			case DRIVING -> "car";
			case WALKING -> "foot";
			case CYCLING -> "bike";
			case TRANSIT -> "transit";
			default -> throw new IllegalArgumentException("OSRM에서 지원하지 않는 경로 타입입니다: " + routeType);
		};
	}

	private String getServerUrl(RouteType routeType) {
		return switch (routeType) {
			case DRIVING -> carServerUrl;
			case WALKING -> footServerUrl;
			case CYCLING -> bikeServerUrl;
			case TRANSIT -> transitServerUrl;
			default -> throw new IllegalArgumentException("OSRM에서 지원하지 않는 경로 타입입니다: " + routeType);
		};
	}

	private RouteResponse convertToRouteResponse(OsrmResponse osrmResponse, RouteType routeType) {
		OsrmRoute route = osrmResponse.getRoutes().getFirst();
		OsrmLeg leg = route.getLegs().getFirst();

		// 좌표 변환 (OSRM polyline 디코딩)
		List<Coordinate> coordinates = new ArrayList<>();

		if (route.getGeometry() != null && !route.getGeometry().trim().isEmpty()) {
			try {
				log.debug("OSRM polyline 디코딩 시작: {}", route.getGeometry());
				coordinates = PolylineDecoder.decode(route.getGeometry());
				log.debug("OSRM polyline 디코딩 완료: {} 개 좌표", coordinates.size());
			} catch (Exception e) {
				log.warn("OSRM polyline 디코딩 실패, 시작/끝점만 사용: {}", e.getMessage());
				// polyline 디코딩 실패 시 시작/끝점만 사용
				if (leg.getSteps() != null && !leg.getSteps().isEmpty()) {
					OsrmStep firstStep = leg.getSteps().get(0);
					OsrmStep lastStep = leg.getSteps().get(leg.getSteps().size() - 1);

					if (firstStep.getManeuver() != null && firstStep.getManeuver().getLocation() != null) {
						coordinates.add(Coordinate.builder()
							.lng(firstStep.getManeuver().getLocation()[0])
							.lat(firstStep.getManeuver().getLocation()[1])
							.build());
					}

					if (lastStep.getManeuver() != null && lastStep.getManeuver().getLocation() != null) {
						coordinates.add(Coordinate.builder()
							.lng(lastStep.getManeuver().getLocation()[0])
							.lat(lastStep.getManeuver().getLocation()[1])
							.build());
					}
				}
			}
		} else {
			log.debug("OSRM geometry가 비어있음, 시작/끝점만 사용");
			// geometry가 없는 경우 시작/끝점만 사용
			if (leg.getSteps() != null && !leg.getSteps().isEmpty()) {
				OsrmStep firstStep = leg.getSteps().get(0);
				OsrmStep lastStep = leg.getSteps().get(leg.getSteps().size() - 1);

				if (firstStep.getManeuver() != null && firstStep.getManeuver().getLocation() != null) {
					coordinates.add(Coordinate.builder()
						.lng(firstStep.getManeuver().getLocation()[0])
						.lat(firstStep.getManeuver().getLocation()[1])
						.build());
				}

				if (lastStep.getManeuver() != null && lastStep.getManeuver().getLocation() != null) {
					coordinates.add(Coordinate.builder()
						.lng(lastStep.getManeuver().getLocation()[0])
						.lat(lastStep.getManeuver().getLocation()[1])
						.build());
				}
			}
		}

		// 단계별 경로 변환
		List<RouteStep> steps = new ArrayList<>();
		if (leg.getSteps() != null) {
			for (OsrmStep step : leg.getSteps()) {
				steps.add(RouteStep.builder()
					.instruction(step.getManeuver().getInstruction())
					.distance(step.getDistance())
					.duration(step.getDuration())
					.startLocation(Coordinate.builder()
						.lng(step.getManeuver().getLocation()[0])
						.lat(step.getManeuver().getLocation()[1])
						.build())
					.endLocation(Coordinate.builder()
						.lng(step.getManeuver().getLocation()[0])
						.lat(step.getManeuver().getLocation()[1])
						.build())
					.build());
			}
		}

		return RouteResponse.builder()
			.coordinates(coordinates)
			.totalDistance(route.getDistance())
			.totalDuration(route.getDuration())
			.routeType(routeType)
			.steps(steps)
			.build();
	}

	// OSRM 응답 DTO 클래스들
	@Setter
	@Getter
	public static class OsrmResponse {
		private String code;
		private List<OsrmRoute> routes;

	}

	@Setter
	@Getter
	public static class OsrmRoute {
		private List<OsrmLeg> legs;
		private double distance;
		private double duration;
		private String geometry; // polyline 문자열

	}

	@Setter
	@Getter
	public static class OsrmLeg {
		private List<OsrmStep> steps;

	}

	@Setter
	@Getter
	public static class OsrmStep {
		private OsrmManeuver maneuver;
		private double distance;
		private double duration;

	}

	@Setter
	@Getter
	public static class OsrmManeuver {
		private String instruction;
		private double[] location;

	}

	@Setter
	@Getter
	public static class OsrmGeometry {
		private String coordinates; // OSRM에서는 polyline으로 인코딩된 문자열을 반환

	}
}
