package com.livelihoodcoupon.route.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.livelihoodcoupon.common.exception.BusinessException;
import com.livelihoodcoupon.common.exception.ErrorCode;
import com.livelihoodcoupon.common.response.CustomApiResponse;
import com.livelihoodcoupon.route.dto.RouteRequest;
import com.livelihoodcoupon.route.dto.RouteResponse;
import com.livelihoodcoupon.route.dto.RouteType;
import com.livelihoodcoupon.route.service.RouteService;

import lombok.RequiredArgsConstructor;

/**
 * 경로 관련 REST API 컨트롤러
 *
 * <h3>주요 기능:</h3>
 * <ul>
 *   <li><code>GET /api/routes/search</code> - 경로 조회</li>
 *   <li><code>GET /api/routes/providers</code> - 사용 가능한 경로 제공자 목록 조회</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/routes")
@RequiredArgsConstructor
public class RouteController {

	private final RouteService routeService;

	/**
	 * 길찾기 API
	 * 출발지와 도착지 좌표를 받아 경로를 조회합니다.
	 *
	 * @param startLng 출발지 경도
	 * @param startLat 출발지 위도
	 * @param endLng 도착지 경도
	 * @param endLat 도착지 위도
	 * @param routeType 경로 타입 (driving, walking, cycling, transit)
	 * @return 경로 정보
	 */
	@GetMapping("/search")
	public ResponseEntity<CustomApiResponse<RouteResponse>> getRoute(
		@RequestParam double startLng,
		@RequestParam double startLat,
		@RequestParam double endLng,
		@RequestParam double endLat,
		@RequestParam(defaultValue = "driving") String routeType) {

		RouteType type = parseRouteType(routeType);
		RouteRequest request = RouteRequest.builder()
			.startLng(startLng)
			.startLat(startLat)
			.endLng(endLng)
			.endLat(endLat)
			.routeType(type)
			.build();

		RouteResponse response = routeService.getRoute(request);
		return ResponseEntity.ok(CustomApiResponse.success(response));
	}

	/**
	 * 경로 제공자 목록 조회 API
	 * 사용 가능한 경로 제공자 목록을 조회합니다.
	 *
	 * @return 제공자 목록
	 */
	@GetMapping("/providers")
	public ResponseEntity<CustomApiResponse<java.util.List<String>>> getProviders() {
		return ResponseEntity.ok(CustomApiResponse.success(routeService.getAvailableProviders()));
	}

	/**
	 * 경로 타입 문자열을 RouteType enum으로 변환
	 */
	private RouteType parseRouteType(String routeTypeStr) {
		return switch (routeTypeStr.toLowerCase()) {
			case "driving" -> RouteType.DRIVING;
			case "walking" -> RouteType.WALKING;
			case "cycling" -> RouteType.CYCLING;
			case "transit" -> RouteType.TRANSIT;
			default -> throw new BusinessException(ErrorCode.UNSUPPORTED_ROUTE_TYPE,
				"지원하지 않는 경로 타입입니다: " + routeTypeStr + ". driving, walking, cycling, transit 중 하나를 선택해주세요.");
		};
	}
}
