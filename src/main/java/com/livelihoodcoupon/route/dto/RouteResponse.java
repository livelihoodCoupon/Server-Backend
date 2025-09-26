package com.livelihoodcoupon.route.dto;

import java.util.List;

import com.livelihoodcoupon.common.dto.Coordinate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 길찾기 응답 DTO
 * 경로 정보, 거리, 소요시간 등을 포함합니다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteResponse {

	/**
	 * 경로 좌표 리스트 (순서대로 연결하면 경로가 됨)
	 */
	private List<Coordinate> coordinates;

	/**
	 * 총 거리 (미터 단위)
	 */
	private double totalDistance;

	/**
	 * 총 소요시간 (초 단위)
	 */
	private double totalDuration;

	/**
	 * 경로 타입
	 */
	private RouteType routeType;

	/**
	 * 경로 상세 정보 (턴바이턴 안내 등)
	 */
	private List<RouteStep> steps;
}
