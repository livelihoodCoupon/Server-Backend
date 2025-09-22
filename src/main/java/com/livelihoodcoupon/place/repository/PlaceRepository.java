package com.livelihoodcoupon.place.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.livelihoodcoupon.place.entity.Place;

/**
 * Place 엔티티에 대한 DB 접근을 담당하는 리포지토리 인터페이스
 * Spring Data JPA를 사용하여 기본적인 CRUD 및 쿼리 메소드를 자동으로 생성함
 */
public interface PlaceRepository extends JpaRepository<Place, Long> {

	/**
	 * 데이터베이스에 존재하는 모든 Place의 placeId를 조회합니다.
	 * @return 모든 placeId 문자열 리스트
	 */
	@Query("SELECT p.placeId FROM Place p")
	List<String> findAllPlaceIds();

	/**
	 * 카카오 장소 ID를 기준으로 Place 엔티티를 조회합니다.
	 * @param placeId 조회할 카카오 장소 ID
	 * @return 조회된 Place 엔티티 (Optional)
	 */
	Optional<Place> findByPlaceId(String placeId);

	/**
	 * 주어진 위도, 경도 및 반경 내에 있는 장소들을 검색하고, 각 장소까지의 거리를 반환합니다.
	 *
	 * @param latitude 검색 중심점의 위도
	 * @param longitude 검색 중심점의 경도
	 * @param radiusMeters 검색 반경 (미터 단위)
	 * @return 검색 조건에 맞는 Place 엔티티와 해당 장소까지의 거리 (미터) 목록
	 */
	@Query(value =
		"SELECT p.id, p.place_id as placeId, p.region, p.place_name as placeName, p.road_address as roadAddress, p.lot_address as lotAddress, p.phone, p.category, p.keyword, p.category_group_code as categoryGroupCode, p.category_group_name as categoryGroupName, p.place_url as placeUrl, ST_Y(p.location::geometry) as lat, ST_X(p.location::geometry) as lng, p.created_at as createdAt, p.updated_at as updatedAt, ST_Distance(p.location, ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)) as distance "
			+ "FROM place p "
			+ "WHERE ST_DWithin(p.location, ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326), :radiusMeters) "
			+ "ORDER BY distance",
		nativeQuery = true)
	List<PlaceWithDistance> findPlacesWithinRadius(double latitude, double longitude, double radiusMeters);
}
