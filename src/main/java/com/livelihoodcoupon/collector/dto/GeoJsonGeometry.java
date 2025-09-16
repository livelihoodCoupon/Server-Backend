package com.livelihoodcoupon.collector.dto;

import java.util.List;

import lombok.Data;

@Data
public class GeoJsonGeometry {
	private String type;
	private List<List<List<Double>>> coordinates;
}
