package com.livelihoodcoupon.collector.dto;

import java.util.List;

import lombok.Data;

@Data
public class GeoJsonFeatureCollection {
	private String type;
	private List<GeoJsonFeature> features;
}
