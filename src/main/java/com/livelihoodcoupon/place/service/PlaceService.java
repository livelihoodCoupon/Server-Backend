package com.livelihoodcoupon.place.service;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.livelihoodcoupon.common.exception.BusinessException;
import com.livelihoodcoupon.common.exception.ErrorCode;
import com.livelihoodcoupon.place.dto.PlaceDetailResponse;
import com.livelihoodcoupon.place.entity.Place;
import com.livelihoodcoupon.place.repository.PlaceRepository;

import lombok.RequiredArgsConstructor;

/**
 * '장소' 도메인의 비즈니스 로직을 처리하는 서비스 클래스
 * 트랜잭션 관리, DTO 변환, 캐싱 적용 등을 수행함
 */
@Service
@RequiredArgsConstructor
public class PlaceService {

	private final PlaceRepository placeRepository;

	/**
	 * 특정 장소의 상세 정보를 조회합니다.
	 * 조회된 결과는 Redis에 캐싱되어 다음 요청 시 더 빠른 응답을 제공합니다.
	 * @param placeId 조회할 카카오 장소 ID
	 * @return 장소 상세 정보 DTO
	 */
	@Transactional(readOnly = true)
	@Cacheable(value = "placeDetails", key = "#placeId", unless = "#result == null")
	public PlaceDetailResponse getPlaceDetails(String placeId) {
		Place place = placeRepository.findByPlaceId(placeId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "장소를 찾을 수 없습니다. ID: " + placeId));
		return toDto(place);
	}

	/**
	 * Place 엔티티를 PlaceDetailResponse DTO로 변환합니다.
	 * @param place 변환할 Place 엔티티 객체
	 * @return 변환된 PlaceDetailResponse DTO 객체
	 */
	private PlaceDetailResponse toDto(Place place) {
		return PlaceDetailResponse.builder()
			.placeId(place.getPlaceId())
			.placeName(place.getPlaceName())
			.roadAddress(place.getRoadAddress())
			.lotAddress(place.getLotAddress())
			.phone(place.getPhone())
			.category(place.getCategory())
			.placeUrl(place.getPlaceUrl())
			// PostGIS의 Point 타입 객체에서 위도(Y)와 경도(X)를 추출하여 DTO에 매핑
			.lat(place.getLocation().getY())
			.lng(place.getLocation().getX())
			.build();
	}
}
