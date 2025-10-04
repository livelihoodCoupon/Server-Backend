package com.livelihoodcoupon.search.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.livelihoodcoupon.search.entity.Category;

@Repository
public interface CategoryRepository extends JpaRepository<Category, String> {

	/**
	 * 상위 top 카테고리 목록
	 * @param pageable
	 * @return
	 */
	Page<Category> findByCategoryNameIsNotNull(Pageable pageable);

	/**
	 * 카테고리이름 존재여부
	 * @param categoryName
	 */
	boolean existsByCategoryNameContaining(String categoryName);

	/**
	 * 카테고리가 있으면  count + 1
	 * @param categoryName
	 * @return
	 */
	@Modifying
	@Transactional
	@Query("update Category set count = count+1 where categoryName = :categoryName")
	int incrementCountNative(String categoryName);

}
