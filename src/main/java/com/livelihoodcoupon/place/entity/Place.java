package com.livelihoodcoupon.place.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import org.locationtech.jts.geom.Point;

import com.livelihoodcoupon.common.entity.BaseEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * '장소' 도메인의 핵심 데이터를 나타내는 엔티티 클래스
 * 데이터베이스의 'place' 테이블과 직접 매핑됨 (테이블명 역시 추후 논의 후 반영 필요)
 */
@Entity
@Table(name = "place")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Place extends BaseEntity {

	// 우선은 Place Entity에 대한 설계가 확정되지 않아서
	// Kakao API로부터 수집한 데이터를 기반으로 모두 담았습니다.
	// 추후 논의 후 우리 서비스에서 필요한 정보만을 저장하도록 삭제/추가 등의 수정이 필요합니다.

	@Column(unique = true, nullable = false)
	private String placeId; // 카카오에서 발급하는 고유 장소 ID (PK 역할)

	private String region;
	private String placeName;
	private String roadAddress;
	private String lotAddress;

	private String phone;
	private String category;
	private String keyword;

	private String categoryGroupCode;
	private String categoryGroupName;
	private String placeUrl;

	// PostGIS 공간 타입을 사용하기 위한 필드: WGS84 좌표계(4326)를 사용하는 Point 데이터를 저장
	@Column(columnDefinition = "geography(Point, 4326)")
	private Point location;
}
