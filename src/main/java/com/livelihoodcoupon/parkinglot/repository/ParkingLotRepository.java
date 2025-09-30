package com.livelihoodcoupon.parkinglot.repository;

import com.livelihoodcoupon.parkinglot.dto.ParkingLotWithDistance;
import com.livelihoodcoupon.parkinglot.entity.ParkingLot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ParkingLotRepository extends JpaRepository<ParkingLot, Long> {

	@Query(nativeQuery = true,
		value = "SELECT *, ST_Distance(location, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography) as distance " +
			"FROM parking_lot " +
			"WHERE ST_DWithin(location, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography, :radius) " +
			"ORDER BY distance ASC")
	List<ParkingLotWithDistance> findNearbyParkingLots(
		@Param("lat") double lat,
		@Param("lng") double lng,
		@Param("radius") int radius
	);
}
