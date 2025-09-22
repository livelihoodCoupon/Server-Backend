package com.livelihoodcoupon.place.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.livelihoodcoupon.common.response.CustomApiResponse;
import com.livelihoodcoupon.place.dto.PlaceDetailResponse;
import com.livelihoodcoupon.place.dto.PlaceSearchResponse;
import com.livelihoodcoupon.place.service.PlaceSearchService;
import com.livelihoodcoupon.place.service.PlaceService;

import lombok.RequiredArgsConstructor;

/**
 * '장소' 관련 HTTP 요청을 처리하는 컨트롤러
 * 클라이언트의 요청을 받아 적절한 서비스 메소드를 호출하고, 그 결과를 응답함
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/places")
public class PlaceController {

	private final PlaceService placeService;
	private final PlaceSearchService placeSearchService;

	/**
	 * 특정 장소의 상세 정보를 조회하는 API 엔드포인트입니다.
	 * @param placeId URL 경로에서 추출한 카카오 장소 ID
	 * @return 표준 API 응답 형식에 담긴 장소 상세 정보
	 */
	@GetMapping("/{placeId}")
	public ResponseEntity<CustomApiResponse<PlaceDetailResponse>> getPlaceDetails(@PathVariable String placeId) {
		PlaceDetailResponse placeDetails = placeService.getPlaceDetails(placeId);
		return ResponseEntity.ok(CustomApiResponse.success(placeDetails));
	}

	/**
	 * 현재 사용자의 위치(위도, 경도)를 기준으로 지정된 반경(킬로미터) 내에 있는 장소들을 검색하고, 각 장소까지의 거리를 함께 반환하는 API 엔드포인트입니다.
	 * @param latitude 사용자의 현재 위도
	 * @param longitude 사용자의 현재 경도
	 * @param radiusKm 검색 반경 (킬로미터 단위, 기본값 1km)
	 * @return 표준 API 응답 형식에 담긴 장소 목록 및 각 장소까지의 거리
	 */
	@GetMapping("/search-by-radius")
	public ResponseEntity<CustomApiResponse<List<PlaceSearchResponse>>> searchPlacesByRadius(
		@RequestParam double latitude,
		@RequestParam double longitude,
		@RequestParam(defaultValue = "1") double radiusKm
	) {
		List<PlaceSearchResponse> places = placeSearchService.searchPlacesByRadius(latitude, longitude, radiusKm);
		return ResponseEntity.ok(CustomApiResponse.success(places));
	}
}
