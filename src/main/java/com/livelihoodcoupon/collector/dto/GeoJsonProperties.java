package com.livelihoodcoupon.collector.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeoJsonProperties {
	// For sig.json
	@JsonProperty("SIG_CD")
	private String sigCd;

	@JsonProperty("SIG_KOR_NM")
	private String sigKorNm;

	// For sido.json
	@JsonProperty("CTPRVN_CD")
	private String ctprvnCd;
}
