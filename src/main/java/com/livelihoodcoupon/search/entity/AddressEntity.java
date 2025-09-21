package com.livelihoodcoupon.search.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Data;

@Data
@Entity
@Table(name = "address")
public class AddressEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name="level")
	private String level;

	@Column(name="category_code")
	private String categoryCode;

	@Column(name="category_name1")
	private String categoryName1;

	@Column(name="category_name2")
	private String categoryName2;

	@Column(name="category_name3")
	private String categoryName3;

}
