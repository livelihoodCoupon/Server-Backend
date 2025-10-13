package com.livelihoodcoupon.parkinglot.service;

import com.livelihoodcoupon.common.exception.BusinessException;
import com.livelihoodcoupon.common.exception.ErrorCode;
import com.livelihoodcoupon.parkinglot.dto.NearbySearchRequest;
import com.livelihoodcoupon.parkinglot.dto.ParkingLotDetailResponse;
import com.livelihoodcoupon.parkinglot.dto.ParkingLotNearbyResponse;
import com.livelihoodcoupon.parkinglot.dto.ParkingLotWithDistance;
import com.livelihoodcoupon.parkinglot.entity.ParkingLot;
import com.livelihoodcoupon.parkinglot.repository.ParkingLotRepository;
import com.livelihoodcoupon.place.entity.Place;
import com.livelihoodcoupon.place.repository.PlaceRepository;
import com.livelihoodcoupon.search.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ParkingLotService {

	private final PlaceRepository placeRepository;
	private final ParkingLotRepository parkingLotRepository;
	private static final int DEFAULT_SEARCH_RADIUS_METER = 500; // 기본 검색 반경 1km
    private static final int MAX_SEARCH_RADIUS_METER = 1000; // 최대 검색 반경 3km

	public PageResponse<ParkingLotNearbyResponse> findNearby(NearbySearchRequest request) {
		int radius = (request.getRadius() == null || request.getRadius() <= 0) ? DEFAULT_SEARCH_RADIUS_METER : request.getRadius();
		if (radius > MAX_SEARCH_RADIUS_METER) {
			radius = MAX_SEARCH_RADIUS_METER;
		}

		Pageable pageable = PageRequest.of(request.getPage() - 1, request.getSize());

		Page<ParkingLotWithDistance> results = parkingLotRepository.findNearbyParkingLots(
			request.getLat(),
			request.getLng(),
			radius,
			pageable
		);

		Page<ParkingLotNearbyResponse> dtoPage = results.map(ParkingLotNearbyResponse::from);

		return new PageResponse<>(dtoPage, request.getSize());
	}

	@Transactional(readOnly = true)
	public List<ParkingLotWithDistance> findNearbyParkingLots(Long placeId) {
		Place place = placeRepository.findById(placeId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

		if (place.getLocation() == null) {
			return Collections.emptyList();
		}

		Pageable pageable = PageRequest.of(0, 10);

		return parkingLotRepository.findNearbyParkingLots(
			place.getLocation().getY(), // Latitude
			place.getLocation().getX(), // Longitude
			DEFAULT_SEARCH_RADIUS_METER,
			pageable
		).getContent();
	}

	@Transactional(readOnly = true)
	public ParkingLotDetailResponse getParkingLotDetails(Long id) {
		ParkingLot parkingLot = parkingLotRepository.findByParkingLotId(id)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "해당 주차장을 찾을 수 없습니다. ID: " + id));

		return ParkingLotDetailResponse.fromParkingLot(parkingLot);
	}
}
