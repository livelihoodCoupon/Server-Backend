package com.livelihoodcoupon.collector.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "place")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
}
