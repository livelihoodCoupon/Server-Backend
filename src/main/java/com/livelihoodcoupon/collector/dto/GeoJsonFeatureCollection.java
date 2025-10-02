package com.livelihoodcoupon.collector.dto;

import java.util.List;

import lombok.Data;

@Data
public class GeoJsonFeatureCollection {
	private String type;
	private List<GeoJsonFeature> features;

	// 🔽 추가
	private List<Double> bbox; // 혹은 double[] 또는 BoundingBox 타입 등 JSON 구조에 맞게

}
