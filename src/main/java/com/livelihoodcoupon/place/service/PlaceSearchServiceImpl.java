package com.livelihoodcoupon.place.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.livelihoodcoupon.common.exception.BusinessException;
import com.livelihoodcoupon.common.exception.ErrorCode;
import com.livelihoodcoupon.place.dto.PlaceSearchResponse;
import com.livelihoodcoupon.place.repository.PlaceRepository;
import com.livelihoodcoupon.place.repository.PlaceWithDistance;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlaceSearchServiceImpl implements PlaceSearchService {

	private static final double EARTH_RADIUS_METERS = 6371000; // 지구 반지름 (미터)
	private final PlaceRepository placeRepository;

	@Override
	@Transactional(readOnly = true)
	public List<PlaceSearchResponse> searchPlacesByRadius(double latitude, double longitude, double radiusKm) {
		// 입력 값 유효성 검사
		if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
			throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "유효하지 않은 위도 또는 경도 값입니다.");
		}
		if (radiusKm <= 0) {
			throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "반경은 0보다 커야 합니다.");
		}

		double radiusMeters = radiusKm * 1000; // 킬로미터를 미터로 변환

		List<PlaceWithDistance> results = placeRepository.findPlacesWithinRadius(latitude, longitude, radiusMeters);

		return results.stream()
			.map(this::toSearchResponseDto)
			.collect(Collectors.toList());
	}

	/**
	 * PlaceWithDistance 객체를 PlaceSearchResponse DTO로 변환합니다.
	 * @param placeWithDistance PlaceWithDistance 객체
	 * @return 변환된 PlaceSearchResponse DTO 객체
	 */
	private PlaceSearchResponse toSearchResponseDto(PlaceWithDistance placeWithDistance) {
		return PlaceSearchResponse.builder()
			.placeId(placeWithDistance.getPlaceId())
			.placeName(placeWithDistance.getPlaceName())
			.roadAddress(placeWithDistance.getRoadAddress())
			.category(placeWithDistance.getCategory())
			.lat(placeWithDistance.getLat()) // 위도
			.lng(placeWithDistance.getLng()) // 경도
			.distance(placeWithDistance.getDistance())
			.build();
	}
}
