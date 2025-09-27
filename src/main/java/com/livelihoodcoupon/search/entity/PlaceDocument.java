package com.livelihoodcoupon.search.entity;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Data;

/**
 * 엘라스틱서치에서 사용하는 index
 * 데이터베이스의 'place' 테이블과 직접 매핑됨 (테이블명 역시 추후 논의 후 반영 필요)
 */
@Data
@Builder
@Document(indexName = "user")
public class PlacesDocument {

	@JsonProperty("place_id")
	private String placeId;

	@JsonProperty("region")
	private String region;

	@JsonProperty("place_name")
	private String placeName;

	@JsonProperty("road_address")
	private String roadAddress;

	@JsonProperty("lot_address")
	private String lotAddress;

	@JsonProperty("phone")
	private String phone;

	@JsonProperty("category")
	private String category;

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
	private Map<String, Double> location;  // { "lat": ..., "lon": ... }
}