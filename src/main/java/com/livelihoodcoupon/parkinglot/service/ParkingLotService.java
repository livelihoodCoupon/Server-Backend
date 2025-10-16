package com.livelihoodcoupon.parkinglot.service;

import com.livelihoodcoupon.common.dto.Coordinate;
import com.livelihoodcoupon.common.exception.BusinessException;
import com.livelihoodcoupon.common.exception.ErrorCode;
import com.livelihoodcoupon.common.service.KakaoApiService;
import com.livelihoodcoupon.parkinglot.dto.NearbySearchRequest;
import com.livelihoodcoupon.parkinglot.dto.ParkingLotDetailResponse;
import com.livelihoodcoupon.parkinglot.dto.ParkingLotNearbyResponse;
import com.livelihoodcoupon.parkinglot.dto.ParkingLotWithDistance;
import com.livelihoodcoupon.parkinglot.repository.ParkingLotRepository;
import com.livelihoodcoupon.search.dto.PageResponse;
import com.livelihoodcoupon.search.dto.SearchRequestDto;
import com.livelihoodcoupon.search.entity.ParkingLotDocument;
import com.livelihoodcoupon.search.service.ElasticParkingLotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParkingLotService {

	private final ParkingLotRepository parkingLotRepository; // For DB-based nearby search
	private final KakaoApiService kakaoApiService;
	private final ElasticParkingLotService elasticParkingLotService; // For ES-based operations

	private static final int DEFAULT_SEARCH_RADIUS_METER = 500;
    private static final int MAX_SEARCH_RADIUS_METER = 1000;

	// This method remains for the old DB-based search endpoint
	public PageResponse<ParkingLotNearbyResponse> searchByQueryOrCoord(SearchRequestDto request) {
		Coordinate searchCenter;

		if (Objects.nonNull(request.getLat()) && Objects.nonNull(request.getLng())) {
			searchCenter = Coordinate.builder().lat(request.getLat()).lng(request.getLng()).build();
		} else if (StringUtils.hasText(request.getQuery())) {

			searchCenter = kakaoApiService.getCoordinatesFromAddress(request.getQuery()).block();
			if (searchCenter == null) {
				throw new BusinessException(ErrorCode.NOT_FOUND, "주소에 해당하는 좌표를 찾을 수 없습니다: " + request.getQuery());
			}
		} else {
			throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "좌표 또는 검색어가 필요합니다.");
		}


		NearbySearchRequest nearbyRequest = new NearbySearchRequest();
		request.initDefaults();

		nearbyRequest.setLat(searchCenter.getLat());
		nearbyRequest.setLng(searchCenter.getLng());
		nearbyRequest.setRadius(request.getRadius());
		nearbyRequest.setPage(request.getPage());

		return findNearby(nearbyRequest);
	}

	// This method remains for the old DB-based search endpoint
	public PageResponse<ParkingLotNearbyResponse> findNearby(NearbySearchRequest request) {
		final double KM_TO_M_THRESHOLD = 100.0;

		Double radiusFromRequest = request.getRadius();
		double radiusInMeters;

		if (radiusFromRequest == null || radiusFromRequest <= 0) {
			radiusInMeters = DEFAULT_SEARCH_RADIUS_METER;
		} else {
			if (radiusFromRequest < KM_TO_M_THRESHOLD) {
				radiusInMeters = radiusFromRequest * 1000; // km -> m conversion
			} else {
				radiusInMeters = radiusFromRequest; // Assume it's already in meters
			}
		}

		if (radiusInMeters > MAX_SEARCH_RADIUS_METER) {
			radiusInMeters = MAX_SEARCH_RADIUS_METER;
		}

		Pageable pageable = PageRequest.of(request.getPage() - 1, request.getSize());

		Page<ParkingLotWithDistance> results = parkingLotRepository.findNearbyParkingLots(
			request.getLat(),
			request.getLng(),
			radiusInMeters,
			pageable
		);

		Page<ParkingLotNearbyResponse> dtoPage = results.map(ParkingLotNearbyResponse::from);

		return new PageResponse<>(dtoPage, request.getSize(), request.getLat(), request.getLng());
	}

	@Transactional(readOnly = true)
	public ParkingLotDetailResponse getParkingLotDetails(Long id) {
		try {
			ParkingLotDocument doc = elasticParkingLotService.getParkingLotById(id.toString());
			if (doc == null) {
				throw new BusinessException(ErrorCode.NOT_FOUND, "해당 주차장을 찾을 수 없습니다. ID: " + id);
			}
			return ParkingLotDetailResponse.fromDocument(doc);
		} catch (IOException e) {
			log.error("Elasticsearch에서 주차장 상세 정보 조회 중 오류 발생. ID: {}", id, e);
			throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "상세 정보 조회 중 오류가 발생했습니다.");
		}
	}
}