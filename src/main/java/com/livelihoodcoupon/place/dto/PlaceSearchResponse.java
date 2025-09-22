package com.livelihoodcoupon.place.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 장소 반경 검색 API의 응답으로 사용될 DTO
 * 클라이언트(프론트엔드)에 필요한 데이터만을 담아 전달하는 역할 (지도 마커 및 검색 결과 리스트 표시용)
 */
@Getter
@Builder
public class PlaceSearchResponse {
	private String placeId;
	private String placeName;
	private String roadAddress;
	private String category;
	private Double lat; // 위도
	private Double lng; // 경도
	private Double distance; // 요청 위치로부터의 거리 (미터 단위)
}
