package com.livelihoodcoupon.search.dto;

import org.springframework.validation.annotation.Validated;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * 검색파라미터를 가져오는 dto
 **/
@Data
@Builder
@Validated
public class SearchRequest {

	//@Schema(description = "페이지번호", example = "1")
	@Builder.Default
	private Integer page = 1;

	//@Schema(description = "검색어", example = "서울시 맛집")
	@Builder.Default
	private String keyword = "";

	//위도
	private double lat;

	//경도
	private double lng;

}