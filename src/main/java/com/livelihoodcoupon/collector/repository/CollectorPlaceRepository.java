package com.livelihoodcoupon.collector.repository;

import java.util.List;
import java.util.stream.Stream;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.livelihoodcoupon.collector.dto.RegionKeywordDto;
import com.livelihoodcoupon.collector.entity.PlaceEntity;

@Repository
public interface CollectorPlaceRepository extends JpaRepository<PlaceEntity, Long> {
	boolean existsByPlaceId(String placeId);

	@Query("SELECT p FROM PlaceEntity p WHERE p.region = :region and p.keyword = :keyword")
	Stream<PlaceEntity> streamByRegionAndKeyword(String region, String keyword);

	@Query("SELECT new com.livelihoodcoupon.collector.dto.RegionKeywordDto(p.region, p.keyword) FROM PlaceEntity p GROUP BY p.region, p.keyword")
	List<RegionKeywordDto> findDistinctRegionAndKeyword();
}
