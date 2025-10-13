package com.livelihoodcoupon.search.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyzedAddress {
	private String fullAddress;
	private String sido;
	private String sigungu;
	private List<SearchToken> resultList;
}
