package com.livelihoodcoupon.collector.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.livelihoodcoupon.collector.service.GeocodeService;
import com.livelihoodcoupon.common.response.CustomApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/geocode")
public class GeocodeController {

	private final GeocodeService geocodeService;

	@PostMapping("/backfill")
	public ResponseEntity<CustomApiResponse<?>> backfill(@RequestParam(defaultValue="500") int limit) {
		int ok = geocodeService.backfill(limit);
		return ResponseEntity.accepted().body(CustomApiResponse.success("backfill accepted: ok=" + ok));
	}
}
