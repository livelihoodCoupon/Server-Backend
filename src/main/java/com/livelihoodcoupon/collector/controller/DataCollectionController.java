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

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/collect")
public class DataCollectionController {

	private final CouponDataCollector collector;
	private final RegionLoader regionLoader;

	@GetMapping("/nationwide")
	public ResponseEntity<String> collectNationwide() {
		try {
			List<RegionData> regions = regionLoader.loadRegions();
			collector.collectForRegions(regions);
			return ResponseEntity.ok("Nationwide data collection started successfully.");
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.internalServerError()
				.body("Error starting nationwide data collection: " + e.getMessage());
		}
	}

	@GetMapping("/{regionName}")
	public ResponseEntity<String> collectForRegionByName(@PathVariable String regionName) {
		try {
			List<RegionData> allRegions = regionLoader.loadRegions();
			Optional<RegionData> targetRegion = allRegions.stream()
				.filter(r -> r.getName().equalsIgnoreCase(regionName))
				.findFirst();

			if (targetRegion.isPresent()) {
				collector.collectForSingleRegion(targetRegion.get());
				return ResponseEntity.ok("Data collection for '" + regionName + "' started successfully.");
			} else {
				return ResponseEntity.badRequest().body("Region '" + regionName + "' not found.");
			}
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.internalServerError()
				.body("Error starting data collection for '" + regionName + "': " + e.getMessage());
		}
	}
}
