package com.livelihoodcoupon.route.dto;

import com.livelihoodcoupon.common.dto.Coordinate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 경로 단계 정보 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteStep {
	private String instruction; // 안내 문구
	private double distance; // 거리 (미터)
	private double duration; // 소요 시간 (초)
	private Coordinate startLocation; // 시작 위치
	private Coordinate endLocation; // 종료 위치
}
