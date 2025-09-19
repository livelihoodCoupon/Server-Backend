package com.livelihoodcoupon.place.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 장소 상세 정보 API의 응답으로 사용될 DTO
 * 클라이언트(프론트엔드)에 필요한 데이터만을 담아 전달하는 역할 (사용 필드 역시 추후 논의 후 반영 필요)
 */
@Getter
@Builder
public class PlaceDetailResponse {
	private String placeId;
	private String placeName;
	private String roadAddress;
	private String lotAddress;
	private String phone;
	private String category;
	private String placeUrl;
	private Double lat; // 위도
	private Double lng; // 경도
}
