package com.livelihoodcoupon.place.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.livelihoodcoupon.place.entity.Place;

/**
 * Place 엔티티에 대한 DB 접근을 담당하는 리포지토리 인터페이스
 * Spring Data JPA를 사용하여 기본적인 CRUD 및 쿼리 메소드를 자동으로 생성함
 */
public interface PlaceRepository extends JpaRepository<Place, Long> {

	/**
	 * 카카오 장소 ID를 기준으로 Place 엔티티를 조회합니다.
	 * @param placeId 조회할 카카오 장소 ID
	 * @return 조회된 Place 엔티티 (Optional)
	 */
	Optional<Place> findByPlaceId(String placeId);
}