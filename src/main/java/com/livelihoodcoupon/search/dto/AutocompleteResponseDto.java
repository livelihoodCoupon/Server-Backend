package com.livelihoodcoupon.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 목록의 검색결과는 담는 dto
 **/
@Getter
@Builder
@AllArgsConstructor
public class AutocompleteResponseDto {

	//@Schema(description = "단어", example = "음식")
	private String word; // Kakao's unique place ID

}
