package com.livelihoodcoupon.search.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.livelihoodcoupon.place.entity.Place;

@Repository
public interface SearchRepository extends JpaRepository<Place, Long>, JpaSpecificationExecutor<Place> {
}
