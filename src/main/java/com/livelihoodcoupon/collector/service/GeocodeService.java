package com.livelihoodcoupon.collector.service;

import java.time.Duration;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.livelihoodcoupon.common.dto.Coordinate;
import com.livelihoodcoupon.common.service.KakaoApiService;
import com.livelihoodcoupon.parkinglot.repository.ParkingLotRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * location(NULL)인 주차장에 대해
 * lotAddress → (실패 시) roadAddress 순으로 카카오 지오코딩 후 location 업데이트.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GeocodeService {

	private final ParkingLotRepository parkingLotRepository;
	private final KakaoApiService kakaoApiService;
	private final GeocodeCsv geocodeCsv; // 이벤트 로그 남기기(원하면 주입 제거해도 됨)

	@Transactional
	public int backfill(int limit) {
		List<ParkingLotRepository.ToGeocode> rows = parkingLotRepository.findTargets(limit);
		int ok = 0;

		for (ParkingLotRepository.ToGeocode r : rows) {
			try {
				Coordinate c = null;
				String used = null;

				if (r.getLotAddress() != null && !r.getLotAddress().trim().isEmpty()) {
					c = kakaoApiService.getCoordinatesFromAddress(r.getLotAddress()).block(Duration.ofSeconds(3));
					used = r.getLotAddress();
				}
				if (c == null && r.getRoadAddress() != null && !r.getRoadAddress().trim().isEmpty()) {
					c = kakaoApiService.getCoordinatesFromAddress(r.getRoadAddress()).block(Duration.ofSeconds(3));
					used = r.getRoadAddress();
				}

				if (c == null) {
					if (geocodeCsv != null) geocodeCsv.noMatch(r.getId(), used, "no_result");
					continue;
				}

				// Kakao: x=lng, y=lat → ST_MakePoint(lng,lat)
				parkingLotRepository.updateLocation(r.getId(), c.getLat(), c.getLng());
				if (geocodeCsv != null) geocodeCsv.ok(r.getId(), used, c.getLat(), c.getLng(), 1.00, "kakao");
				ok++;
			} catch (Exception e) {
				if (geocodeCsv != null) geocodeCsv.error(r.getId(), null, e.getMessage());
				log.warn("geocode error id={}: {}", r.getId(), e.toString());
			}
		}
		return ok;
	}
}
