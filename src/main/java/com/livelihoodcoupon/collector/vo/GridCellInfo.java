package com.livelihoodcoupon.collector.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GridCellInfo {
	// Fields for the new recursive approach
	private double lat;
	private double lng;
	private int radius;
	private int recursionDepth;
	private int totalCount;
	private int pageableCount;
	private boolean isDense; // Flag to indicate if this cell was split
}