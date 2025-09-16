package com.livelihoodcoupon.collector.vo;

import java.util.List;

import lombok.Data;

@Data
public class RegionData {
	private String name;
	private List<List<Double>> polygon;
}