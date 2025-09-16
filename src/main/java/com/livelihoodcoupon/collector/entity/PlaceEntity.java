package com.livelihoodcoupon.collector.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.livelihoodcoupon.collector.dto.KakaoPlace;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "place")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlaceEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(unique = true, nullable = false)
	private String placeId; // Kakao's unique place ID

	private String region;
	private String placeName;
	private String roadAddress;
	private String lotAddress;
	private Double lat;
	private Double lng;
	private String phone;
	private String category;
	private String keyword;

	// New fields from API documentation
	private String categoryGroupCode;
	private String categoryGroupName;
	private String placeUrl;
	private String distance;

	public PlaceEntity(KakaoPlace place, String region, String keyword) {
		this.placeId = place.getId();
		this.region = region;
		this.placeName = place.getPlaceName();
		this.roadAddress = place.getRoadAddressName();
		this.lotAddress = place.getAddressName();
		this.lat = Double.parseDouble(place.getY());
		this.lng = Double.parseDouble(place.getX());
		this.phone = place.getPhone();
		this.category = place.getCategoryName();
		this.keyword = keyword;

		// Mapping new fields
		this.categoryGroupCode = place.getCategoryGroupCode();
		this.categoryGroupName = place.getCategoryGroupName();
		this.placeUrl = place.getPlaceUrl();
		this.distance = place.getDistance();
	}
}
