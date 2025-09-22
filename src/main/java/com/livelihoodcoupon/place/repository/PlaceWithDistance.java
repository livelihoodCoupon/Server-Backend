package com.livelihoodcoupon.place.repository;

import java.time.OffsetDateTime;

public interface PlaceWithDistance {
	Long getId();

	String getPlaceId();

	String getRegion();

	String getPlaceName();

	String getRoadAddress();

	String getLotAddress();

	String getPhone();

	String getCategory();

	String getKeyword();

	String getCategoryGroupCode();

	String getCategoryGroupName();

	String getPlaceUrl();

	Double getLat(); // Latitude

	Double getLng(); // Longitude

	OffsetDateTime getCreatedAt();

	OffsetDateTime getUpdatedAt();

	Double getDistance(); // The calculated distance
}
