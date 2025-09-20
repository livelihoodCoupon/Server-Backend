package com.livelihoodcoupon.search.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.livelihoodcoupon.collector.entity.PlaceEntity;

public interface SearchRepository extends JpaRepository<PlaceEntity, Long> {
}
