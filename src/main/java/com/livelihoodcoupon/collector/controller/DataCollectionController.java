package com.livelihoodcoupon.collector.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.livelihoodcoupon.collector.service.CouponDataCollector;
import com.livelihoodcoupon.collector.service.RegionLoader;
import com.livelihoodcoupon.collector.vo.RegionData;
import com.livelihoodcoupon.common.exception.BusinessException;
import com.livelihoodcoupon.common.exception.ErrorCode;
import com.livelihoodcoupon.common.response.CustomApiResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 데이터 수집 관련 API를 제공하는 컨트롤러
 *
 * <h3>주요 기능:</h3>
 * <ul>
 *     <li><b>전국 데이터 수집:</b> 모든 지역에 대한 소비쿠폰 장소 데이터 수집</li>
 *     <li>
 *       <b>지역별 데이터 수집:</b> 특정 지역에 대한 소비쿠폰 장소 데이터 수집
 *       <ul>
 *         <li><b>지역 검증:</b> 요청된 지역명이 유효한지 확인</li>
 *       </ul>
 *     </li>
 *  </ul>
 *
 * <h3>API 엔드포인트:</h3>
 * <ul>
 *   <li><code>GET /admin/collect/nationwide</code> - 전국 데이터 수집 시작</li>
 *   <li><code>GET /admin/collect/{regionName}</code> - 특정 지역 데이터 수집 시작</li>
 * </ul>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/collect")
public class DataCollectionController {

	/** 소비쿠폰 데이터 수집 서비스 */
	private final CouponDataCollector collector;

	/** 지역 정보 로더 서비스 */
	private final RegionLoader regionLoader;

	/**
	 * 전국 데이터 수집 시작
	 *
	 * <p>모든 지역에 대한 소비쿠폰 장소 데이터를 수집합니다.
	 * 이 작업은 비동기적으로 실행되며, 완료까지 상당한 시간이 소요될 수 있습니다.</p>
	 *
	 * @return 수집 시작 성공 메시지
	 * @throws BusinessException 데이터 수집 시작 중 오류 발생 시
	 */
	@GetMapping("/nationwide")
	public ResponseEntity<CustomApiResponse<?>> collectNationwide() {
		try {
			List<RegionData> regions = regionLoader.loadRegions();
			collector.collectForRegions(regions);
			return ResponseEntity.ok(CustomApiResponse.success("Nationwide data collection started successfully."));
		} catch (Exception e) {
			log.error("Error starting nationwide data collection: {}", e.getMessage(), e);
			throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR,
				"Error starting nationwide data collection: " + e.getMessage());
		}
	}

	/**
	 * 특정 지역 데이터 수집 시작
	 *
	 * <p>지정된 지역명에 해당하는 지역의 소비쿠폰 장소 데이터를 수집합니다.
	 * 지역명은 대소문자를 구분하지 않으며, 정확한 지역명을 입력해야 합니다.</p>
	 *
	 * @param regionName 수집할 지역명 (예: "서울특별시 종로구", "경상남도 창원시 의창구")
	 * @return 수집 시작 성공 메시지
	 * @throws BusinessException 지역을 찾을 수 없거나 데이터 수집 중 오류 발생 시
	 */
	@GetMapping("/{regionName}")
	public ResponseEntity<CustomApiResponse<?>> collectForRegionByName(@PathVariable String regionName) {
		try {
			List<RegionData> allRegions = regionLoader.loadRegions();
			Optional<RegionData> targetRegion = allRegions.stream()
				.filter(r -> r.getName().equalsIgnoreCase(regionName))
				.findFirst();

			if (targetRegion.isPresent()) {
				collector.collectForSingleRegion(targetRegion.get());
				return ResponseEntity.ok(
					CustomApiResponse.success("Data collection for '" + regionName + "' started successfully."));
			} else {
				throw new BusinessException(ErrorCode.NOT_FOUND, "Region '" + regionName + "' not found.");
			}
		} catch (BusinessException e) {
			throw e;
		} catch (Exception e) {
			log.error("Error starting data collection for '{}': {}", regionName, e.getMessage(), e);
			throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR,
				"Error starting data collection for '" + regionName + "': " + e.getMessage());
		}
	}
}