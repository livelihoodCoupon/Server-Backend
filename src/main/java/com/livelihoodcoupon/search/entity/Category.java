package com.livelihoodcoupon.search.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import com.livelihoodcoupon.common.entity.BaseEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Category extends BaseEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY) // 자동 증가
	@Column(name = "id", unique = true, nullable = false)
	private Long id;

	@Column(name = "category_name")
	private String categoryName;

	@Column(name = "count")
	private int count;
}
