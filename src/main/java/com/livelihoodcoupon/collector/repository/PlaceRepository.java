package com.livelihoodcoupon.collector.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.livelihoodcoupon.collector.entity.PlaceEntity;

@Repository
public interface PlaceRepository extends JpaRepository<PlaceEntity, Long> {
	boolean existsByPlaceId(String placeId);
}
