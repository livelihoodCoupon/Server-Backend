package com.livelihoodcoupon.parkinglot.service;

import com.livelihoodcoupon.common.exception.BusinessException;
import com.livelihoodcoupon.common.exception.ErrorCode;
import com.livelihoodcoupon.parkinglot.dto.NearbySearchRequest;
import com.livelihoodcoupon.parkinglot.dto.ParkingLotNearbyResponse;
import com.livelihoodcoupon.parkinglot.dto.ParkingLotWithDistance;
import com.livelihoodcoupon.parkinglot.repository.ParkingLotRepository;
import com.livelihoodcoupon.place.entity.Place;
import com.livelihoodcoupon.place.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ParkingLotService {

	private final PlaceRepository placeRepository;
	private final ParkingLotRepository parkingLotRepository;
	private static final int DEFAULT_SEARCH_RADIUS_METER = 1000; // 기본 검색 반경 1km
    private static final int MAX_SEARCH_RADIUS_METER = 3000; // 최대 검색 반경 3km

    /**
     * 요청받은 좌표와 반경을 기준으로 주변 주차장을 검색합니다.
     * @param request 위도, 경도, 반경을 담은 요청 DTO
     * @return 거리순으로 정렬된 주차장 목록 DTO
     */
    @Transactional(readOnly = true)
    public List<ParkingLotNearbyResponse> findNearby(NearbySearchRequest request) {
        int radius = (request.getRadius() == null || request.getRadius() <= 0) ? DEFAULT_SEARCH_RADIUS_METER : request.getRadius();
        if (radius > MAX_SEARCH_RADIUS_METER) {
            radius = MAX_SEARCH_RADIUS_METER;
        }

        List<ParkingLotWithDistance> results = parkingLotRepository.findNearbyParkingLots(
                request.getLat(),
                request.getLng(),
                radius
        );

        return results.stream()
                .map(ParkingLotNearbyResponse::from)
                .collect(Collectors.toList());
    }

	@Transactional(readOnly = true)
	public List<ParkingLotWithDistance> findNearbyParkingLots(Long placeId) {
		Place place = placeRepository.findById(placeId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

		if (place.getLocation() == null) {
			return Collections.emptyList();
		}

		return parkingLotRepository.findNearbyParkingLots(
			place.getLocation().getY(), // Latitude
			place.getLocation().getX(), // Longitude
			DEFAULT_SEARCH_RADIUS_METER
		);
	}
}
