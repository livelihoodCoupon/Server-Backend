package com.livelihoodcoupon.collector.dto;

import lombok.Data;

@Data
public class GeoJsonFeature {
	private String type;
	private GeoJsonProperties properties;
	private GeoJsonGeometry geometry;
}
