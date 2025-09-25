package com.livelihoodcoupon.route.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 좌표 정보 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Coordinate {
    private double lng; // 경도 (X 좌표)
    private double lat; // 위도 (Y 좌표)
}
