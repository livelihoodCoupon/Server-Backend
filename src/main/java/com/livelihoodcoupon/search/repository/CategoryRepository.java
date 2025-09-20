package com.livelihoodcoupon.search.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.livelihoodcoupon.search.entity.AddressEntity;
import com.livelihoodcoupon.search.entity.CategoryEntity;

public interface CategoryRepository extends JpaRepository<CategoryEntity, Long> {
}
