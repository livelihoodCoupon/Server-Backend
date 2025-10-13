package com.livelihoodcoupon.search.dto;

import java.util.Objects;
import java.util.Optional;

import org.springframework.validation.annotation.Validated;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 검색파라미터를 가져오는 dto
 **/
@Data
@Builder
@Validated
@NoArgsConstructor
@AllArgsConstructor
public class SearchRequestDto {

	//@Schema(description = "페이지번호", example = "1")
	@Builder.Default
	private Integer page = 1;

	//@Schema(description = "검색어", example = "서울시 종로구 냉면 맛집")
	@Builder.Default
	// @NotBlank(message = "검색어는 필수입니다.")
	private String query = "";

	//위도
	@Builder.Default
	private Double lat = 37.560949118173454;

	//경도
	@Builder.Default
	private Double lng = 126.9863813979137;

	//사용자 현재 위도 (GPS 기반)
	private Double userLat;

	//사용자 현재 경도 (GPS 기반)
	private Double userLng;

	//거리 1km 기본
	@Builder.Default
	private Double radius = 1.0;

	//정렬기준
	@Builder.Default
	private String sort = "distance";

	//위치 기반 검색 강제 여부 (true일 경우 검색어 내 지역 정보 무시)
	@Builder.Default
	private boolean forceLocationSearch = false;

	public void initDefaults() {
		page = (page == null || page == 0) ? 0 : page;
		lat = (lat == null || lat == 0.0) ? 37.560949118173454 : lat;
		lng = (lng == null || lng == 0.0) ? 126.9863813979137 : lng;
		radius = (radius == null || Optional.of(radius).orElse(0.0) == 0.0) ? 1000 : radius;
		sort = (sort == null || Objects.equals(sort, "")) ? "distance" : sort;
	}

	//서울 명동위치
	//위도 37.560949118173454
	//경도 126.9863813979137

	//서울시 종로 5가 위치
	//위도 : 37.57104033689386
	//경도: 127.0019782463416
}
