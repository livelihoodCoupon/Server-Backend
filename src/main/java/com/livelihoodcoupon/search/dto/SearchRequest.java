package com.livelihoodcoupon.search.dto;

import org.springframework.validation.annotation.Validated;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
@Validated
public class SearchRequest {

	//@Schema(description = "페이지번호", example = "1")
	@Builder.Default
	private Integer page = 1;

	//@Schema(description = "페이지사이즈", example = "10")
	@Builder.Default
	private Integer pageSize = 10;

	//@Schema(description = "검색어", example = "서울시 맛집")
	@Builder.Default
	private String keyword = "";
}