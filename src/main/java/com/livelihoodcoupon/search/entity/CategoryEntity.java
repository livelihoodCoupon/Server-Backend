package com.livelihoodcoupon.collector.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.livelihoodcoupon.collector.dto.KakaoPlace;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "local_cate")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LocalPlaceEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column
	private String place_cate;

	@Column
	private String place_name; // Kakao's unique place ID

	@Column
	private boolean isDown;

}
