package com.livelihoodcoupon.search.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.livelihoodcoupon.collector.entity.PlaceEntity;
import com.livelihoodcoupon.search.entity.AddressEntity;

public interface AddressRepository extends JpaRepository<AddressEntity, Long> {
}
