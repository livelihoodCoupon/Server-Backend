package com.livelihoodcoupon.search.dto;

import jakarta.validation.constraints.NotBlank;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Builder
public class AutocompleteDto {

	@NotBlank(message = "검색어는 필수입니다.")
	private String word;

	public AutocompleteDto(String word) {
		this.word = word;
	}
}
