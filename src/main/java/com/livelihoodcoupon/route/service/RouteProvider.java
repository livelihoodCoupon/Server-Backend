package com.livelihoodcoupon.route.service;

import com.livelihoodcoupon.route.dto.RouteRequest;
import com.livelihoodcoupon.route.dto.RouteResponse;
import com.livelihoodcoupon.route.dto.RouteType;

/**
 * 경로 서비스 추상화 인터페이스
 * 다양한 경로 제공자(카카오, OSRM 등)를 통합하기 위한 인터페이스
 */
public interface RouteProvider {

	/**
	 * 경로를 조회합니다.
	 *
	 * @param request 경로 요청 정보
	 * @return 경로 응답 정보
	 */
	RouteResponse getRoute(RouteRequest request);

	/**
	 * 해당 경로 타입을 지원하는지 확인합니다.
	 *
	 * @param routeType 경로 타입
	 * @return 지원 여부
	 */
	boolean supports(RouteType routeType);

	/**
	 * 제공자 이름을 반환합니다.
	 *
	 * @return 제공자 이름
	 */
	String getProviderName();
}
