package com.livelihoodcoupon.collector.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import com.livelihoodcoupon.common.entity.BaseEntity;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class ScannedGrid extends BaseEntity {

	@Column(nullable = false)
	private String regionName;

	@Column(nullable = false)
	private String keyword;

	@Column(nullable = false)
	private double gridCenterLat;

	@Column(nullable = false)
	private double gridCenterLng;

	@Column(nullable = false)
	private int gridRadius;
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private GridStatus status;

	@Builder
	public ScannedGrid(String regionName, String keyword, double gridCenterLat, double gridCenterLng, int gridRadius,
		GridStatus status) {
		this.regionName = regionName;
		this.keyword = keyword;
		this.gridCenterLat = gridCenterLat;
		this.gridCenterLng = gridCenterLng;
		this.gridRadius = gridRadius;
		this.status = status;
	}

	public enum GridStatus {
		COMPLETED,  // 수집 완료
		SUBDIVIDED  // 하위 격자로 분할됨
	}
}
