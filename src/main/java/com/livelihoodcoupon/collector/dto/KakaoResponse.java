package com.livelihoodcoupon.collector.dto;

import java.util.List;

import lombok.Data;

@Data
public class KakaoResponse {
	private List<KakaoPlace> documents;
	private KakaoMeta meta;
}
