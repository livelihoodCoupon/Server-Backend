package com.livelihoodcoupon.collector.repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.livelihoodcoupon.collector.dto.RegionKeywordDto;
import com.livelihoodcoupon.collector.entity.ScannedGrid;

public interface ScannedGridRepository extends JpaRepository<ScannedGrid, Long> {

	Optional<ScannedGrid> findByRegionNameAndKeywordAndGridCenterLatAndGridCenterLngAndGridRadius(
		String regionName, String keyword, double lat, double lng, int radius);

	Stream<ScannedGrid> findByRegionNameAndKeyword(String regionName, String keyword);

	@Query("SELECT new com.livelihoodcoupon.collector.dto.RegionKeywordDto(s.regionName, s.keyword) FROM ScannedGrid s GROUP BY s.regionName, s.keyword")
	List<RegionKeywordDto> findDistinctRegionAndKeyword();
}
