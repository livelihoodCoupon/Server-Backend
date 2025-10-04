package com.livelihoodcoupon.search.dto;

import com.livelihoodcoupon.search.entity.Category;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CategoryDto {
	private String categoryName;

	public CategoryDto(Category category) {
		super();
		this.categoryName = category.getCategoryName();
	}
}
