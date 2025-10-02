package com.livelihoodcoupon.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class KakaoPlace {
	private String id;

	@JsonProperty("place_name")
	private String placeName;

	@JsonProperty("category_name")
	private String categoryName;

	@JsonProperty("category_group_code")
	private String categoryGroupCode;

	@JsonProperty("category_group_name")
	private String categoryGroupName;

	private String phone;

	@JsonProperty("address_name")
	private String addressName;

	@JsonProperty("road_address_name")
	private String roadAddressName;

	private String x; // longitude
	private String y; // latitude

	@JsonProperty("place_url")
	private String placeUrl;

	private String distance;
}
