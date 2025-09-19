package com.livelihoodcoupon.batch;

import lombok.Data;

@Data
public class PlaceCsvDto {
	private String placeId;
	private String region;
	private String placeName;
	private String roadAddress;
	private String lotAddress;
	private double lat; // latitude
	private double lng; // longitude
	private String phone;
	private String categoryName;
	private String keyword;
	private String categoryGroupCode;
	private String categoryGroupName;
	private String placeUrl;
}
