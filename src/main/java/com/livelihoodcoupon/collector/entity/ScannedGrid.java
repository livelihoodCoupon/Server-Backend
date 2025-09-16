package com.livelihoodcoupon.collector.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
public class ScannedGrid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    public enum GridStatus {
        COMPLETED,  // 수집 완료
        SUBDIVIDED  // 하위 격자로 분할됨
    }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GridStatus status;

    private LocalDateTime createdAt;

    @Builder
    public ScannedGrid(String regionName, String keyword, double gridCenterLat, double gridCenterLng, int gridRadius, GridStatus status) {
        this.regionName = regionName;
        this.keyword = keyword;
        this.gridCenterLat = gridCenterLat;
        this.gridCenterLng = gridCenterLng;
        this.gridRadius = gridRadius;
        this.status = status;
        this.createdAt = LocalDateTime.now();
    }
}
