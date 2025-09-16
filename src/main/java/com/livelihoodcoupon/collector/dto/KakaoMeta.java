package com.livelihoodcoupon.collector.dto;

import lombok.Data;

@Data
public class KakaoMeta {
	private int total_count;
	private int pageable_count;
	private boolean is_end;
}
