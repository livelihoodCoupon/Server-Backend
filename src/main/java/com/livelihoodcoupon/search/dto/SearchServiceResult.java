package com.livelihoodcoupon.search.dto;

import org.springframework.data.domain.Page;

import lombok.Getter;

@Getter
public class SearchServiceResult {
	private final Page<PlaceSearchResponseDto> page;
	private final double searchCenterLat;
	private final double searchCenterLng;

	public SearchServiceResult(Page<PlaceSearchResponseDto> page, double searchCenterLat, double searchCenterLng) {
		this.page = page;
		this.searchCenterLat = searchCenterLat;
		this.searchCenterLng = searchCenterLng;
	}
}
