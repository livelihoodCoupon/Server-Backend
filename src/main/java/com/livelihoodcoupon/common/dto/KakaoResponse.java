package com.livelihoodcoupon.common.dto;

import java.util.List;

import lombok.Data;

@Data
public class KakaoResponse {
	private List<KakaoPlace> documents;
	private KakaoMeta meta;
}
