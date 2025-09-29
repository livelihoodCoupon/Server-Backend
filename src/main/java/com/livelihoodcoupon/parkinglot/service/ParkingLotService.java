package com.livelihoodcoupon.parkinglot.service;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.livelihoodcoupon.common.exception.BusinessException;
import com.livelihoodcoupon.parkinglot.dto.ParkingLotWithDistance;
import com.livelihoodcoupon.parkinglot.repository.ParkingLotRepository;
import com.livelihoodcoupon.place.entity.Place;
import com.livelihoodcoupon.place.repository.PlaceRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ParkingLotService {

	private final PlaceRepository placeRepository;
	private final ParkingLotRepository parkingLotRepository;
	private static final int SEARCH_RADIUS_METER = 500; // 500미터 반경

	@Transactional(readOnly = true)
	public List<ParkingLotWithDistance> findNearbyParkingLots(Long placeId) {
		Place place = placeRepository.findById(placeId)
			.orElseThrow(() -> new BusinessException(ErrorCode.PLACE_NOT_FOUND));

		if (place.getLocation() == null) {
			return Collections.emptyList();
		}

		return parkingLotRepository.findNearbyParkingLots(
			place.getLocation().getY(), // Latitude
			place.getLocation().getX(), // Longitude
			SEARCH_RADIUS_METER
		);
	}
}
