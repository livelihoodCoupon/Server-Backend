package com.livelihoodcoupon.route.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 길찾기 요청 DTO
 * 출발지와 도착지 좌표, 경로 타입을 포함합니다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteRequest {

	/**
	 * 출발지 경도 (X 좌표)
	 */
	private double startLng;

	/**
	 * 출발지 위도 (Y 좌표)
	 */
	private double startLat;

	/**
	 * 도착지 경도 (X 좌표)
	 */
	private double endLng;

	/**
	 * 도착지 위도 (Y 좌표)
	 */
	private double endLat;

	/**
	 * 경로 타입 (자동차, 도보, 자전거, 대중교통)
	 */
	private RouteType routeType;

}
