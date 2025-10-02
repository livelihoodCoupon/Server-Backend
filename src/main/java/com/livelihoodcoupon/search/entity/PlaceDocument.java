package com.livelihoodcoupon.search.entity;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.livelihoodcoupon.common.dto.Coordinate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 엘라스틱서치에서 사용하는 index
 * 데이터베이스의 'place' 테이블과 직접 매핑됨 (테이블명 역시 추후 논의 후 반영 필요)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlaceDocument {

	@JsonProperty("id")
	private int id;

	@JsonProperty("place_id")
	private String placeId;

	@JsonProperty("region")
	private String region;

	@JsonProperty("place_name")
	private String placeName;

	@JsonProperty("road_address_sido")
	private String roadAddressSido;

	@JsonProperty("road_address_sigungu")
	private String roadAddressSigungu;

	@JsonProperty("road_address_road")
	private String roadAddressRoad;

	@JsonProperty("road_address_dong")
	private String roadAddressDong;

	@JsonProperty("road_address")
	private String roadAddress;

	@JsonProperty("lot_address")
	private String lotAddress;

	@JsonProperty("phone")
	private String phone;

	@JsonProperty("category_level1")
	private String categoryLevel1;

	@JsonProperty("category_level2")
	private String categoryLevel2;

	@JsonProperty("category_level3")
	private String categoryLevel3;

	@JsonProperty("category_level4")
	private String categoryLevel4;

	@JsonProperty("category")
	private String category;

	@JsonProperty("distance")
	private Float distance;

	@JsonProperty("keyword")
	private String keyword;

	@JsonProperty("category_group_code")
	private String categoryGroupCode;

	@JsonProperty("category_group_name")
	private String categoryGroupName;

	@JsonProperty("place_url")
	private String placeUrl;

	// Elasticsearch의 geo_point 타입에 맞게 lat/lon 구조로 선언
	@JsonProperty("location")
	private Coordinate location;

	@JsonProperty("created_at")
	private OffsetDateTime created_at;

	@JsonProperty("updated_at")
	private OffsetDateTime updated_at;

}
