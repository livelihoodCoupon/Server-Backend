package com.livelihoodcoupon.search.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.livelihoodcoupon.search.dto.CategoryDto;
import com.livelihoodcoupon.search.entity.Category;
import com.livelihoodcoupon.search.repository.CategoryRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class CategoryService {

	private final CategoryRepository categoryRepository;

	public CategoryService(CategoryRepository categoryRepository) {
		this.categoryRepository = categoryRepository;
	}

	/**
	 * 카테고리 상위 10개 가져오기
	 * @param pageSize
	 * @return
	 */
	public List<CategoryDto> findTopCategory(int pageSize) {
		int page = 0;
		pageSize = pageSize > 0 ? pageSize : 10;
		Pageable pageable = PageRequest.of(page, pageSize, Sort.by("count").descending());
		return categoryRepository.findByCategoryNameIsNotNull(pageable).stream()
			.map(CategoryDto::new).collect(Collectors.toList());
	}

	/**
	 * 카테고리 이름 비교해서 있으면 count 업데이터 없으면 등록
	 * @param categoryName 카테고리명
	 */
	@Transactional
	public void saveOrIncrement(String categoryName) {
		boolean exists = categoryRepository.existsByCategoryNameContaining(categoryName);
		if (!exists) { //카테고리이름이 없으면 등록
			Category category = Category.builder().categoryName(categoryName).count(1).build();
			categoryRepository.save(category);
		} else { //있으면 count+1
			categoryRepository.incrementCountNative(categoryName);
		}
	}

	/**
	 * 전체갯수
	 * @return
	 */
	public long isExistCount() {
		return categoryRepository.count();
	}

	/**
	 * caregory 전체 지우기
	 */
	public void deleteCategory() {
		categoryRepository.deleteAll();
	}
}
