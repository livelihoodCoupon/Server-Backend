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

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/collect")
public class DataCollectionController {

	private final CouponDataCollector collector;
	private final RegionLoader regionLoader;

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
