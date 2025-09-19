package com.livelihoodcoupon.place.repository;

import static org.assertj.core.api.Assertions.*;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.livelihoodcoupon.place.entity.Place;

/**
 * 목표: PlaceRepository가 H2(PostgreSQL 모드)와 잘 연동되어 Place 엔티티(특히 Point 타입)를 정확하게 저장하고 조회하는지 검증합니다.
 * @DataJpaTest: JPA 관련 설정만 로드하여 DB 테스트에 최적화된 환경을 제공합니다.
 * - 내장된 H2 데이터베이스를 사용하며, 각 테스트는 트랜잭션 내에서 실행 후 롤백되어 독립성을 보장합니다.
 */
@DataJpaTest // JPA 관련 컴포넌트만 테스트하기 위한 설정
public class PlaceRepositoryTest {

	private final GeometryFactory geometryFactory = new GeometryFactory();
	@Autowired
	private PlaceRepository placeRepository;

	// 현재는 H2 인메모리 사용해 GEOGRAPHY 타입을 지원하지 않으므로 이 테스트는 건너뜀
	// 실제 DB(GEOGRAPHY 타입 지원)로 교체 시 이 테스트 적용하면 됨
	// @Test
	@DisplayName("Place 엔티티 저장 및 placeId로 조회 SUCCESS")
	void saveAndFindByPlaceId_success() {
		// given: 테스트 준비 (Place 엔티티 - 특히 PostGIS의 Point 타입이 잘 처리되는지 확인할 수 있도록 세팅)
		String placeId = "12345";
		Point testLocation = geometryFactory.createPoint(new Coordinate(127.123, 37.456));
		Place newPlace = Place.builder()
			.placeId(placeId)
			.placeName("테스트 장소")
			.location(testLocation)
			.build();

		// when: 저장 및 조회 시
		placeRepository.save(newPlace);
		Optional<Place> foundPlaceOptional = placeRepository.findByPlaceId(placeId);

		// then: 다음을 검증한다.
		assertThat(foundPlaceOptional).isPresent(); // 조회 결과가 있는지
		Place foundPlace = foundPlaceOptional.get();
		assertThat(foundPlace.getPlaceId()).isEqualTo(placeId); // 처음 준비한 데이터와 정보가 일치하는지
		assertThat(foundPlace.getPlaceName()).isEqualTo("테스트 장소");
		assertThat(foundPlace.getLocation().getX()).isEqualTo(127.123); // Point 타입의 좌표가 정확하게 저장되고 조회되는지
		assertThat(foundPlace.getLocation().getY()).isEqualTo(37.456);
	}
}
