package com.livelihoodcoupon.common.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class Coord2RegionCodeResponse {
	private Meta meta;
	private List<RegionDocument> documents;

	@Data
	public static class Meta {
		@JsonProperty("total_count")
		private int totalCount;
	}

	@Data
	public static class RegionDocument {
		@JsonProperty("region_type")
		private String regionType;

		@JsonProperty("address_name")
		private String addressName;

		@JsonProperty("region_1depth_name")
		private String region1DepthName;

		@JsonProperty("region_2depth_name")
		private String region2DepthName;

		@JsonProperty("region_3depth_name")
		private String region3DepthName;

		@JsonProperty("region_4depth_name")
		private String region4DepthName;

		private String code;
		private double x; // longitude
		private double y; // latitude
	}
}
