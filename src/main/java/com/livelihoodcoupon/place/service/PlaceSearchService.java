package com.livelihoodcoupon.place.service;

import java.util.List;

import com.livelihoodcoupon.place.dto.PlaceSearchResponse;

public interface PlaceSearchService {

	/**
	 * 주어진 위도, 경도 및 반경 내에 있는 장소들을 검색하고, 각 장소까지의 거리를 반환합니다.
	 *
	 * @param latitude 검색 중심점의 위도
	 * @param longitude 검색 중심점의 경도
	 * @param radiusKm 검색 반경 (킬로미터 단위)
	 * @return 검색 조건에 맞는 PlaceSearchResponse DTO 목록
	 */
	List<PlaceSearchResponse> searchPlacesByRadius(double latitude, double longitude, double radiusKm);
}
