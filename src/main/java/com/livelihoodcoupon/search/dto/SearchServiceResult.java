package com.livelihoodcoupon.search.dto;

import lombok.Getter;
import org.springframework.data.domain.Page;

@Getter
public class SearchServiceResult<T> {
	private final Page<T> page;
	private final double searchCenterLat;
	private final double searchCenterLng;

	public SearchServiceResult(Page<T> page, double searchCenterLat, double searchCenterLng) {
		this.page = page;
		this.searchCenterLat = searchCenterLat;
		this.searchCenterLng = searchCenterLng;
	}
}
