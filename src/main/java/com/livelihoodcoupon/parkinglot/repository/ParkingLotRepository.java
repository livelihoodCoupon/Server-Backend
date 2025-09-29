package com.livelihoodcoupon.parkinglot.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.livelihoodcoupon.parkinglot.dto.ParkingLotWithDistance;
import com.livelihoodcoupon.parkinglot.entity.ParkingLot;

public interface ParkingLotRepository extends JpaRepository<ParkingLot, Long> {
	@Query(value = """
        SELECT 
            p.*, 
            ST_Distance(p.location, ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography) as distance
        FROM 
            parking_lot p
        WHERE 
            ST_DWithin(
                p.location, 
                ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography, 
                :distanceMeter
            )
        ORDER BY distance ASC
        """, nativeQuery = true)
	List<ParkingLotWithDistance> findNearbyParkingLots(
		@Param("latitude") double latitude,
		@Param("longitude") double longitude,
		@Param("distanceMeter") int distanceMeter
	);
}
