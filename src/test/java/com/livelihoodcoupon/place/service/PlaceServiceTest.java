package com.livelihoodcoupon.place.service;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.livelihoodcoupon.common.exception.BusinessException;
import com.livelihoodcoupon.place.dto.PlaceDetailResponse;
import com.livelihoodcoupon.place.entity.Place;
import com.livelihoodcoupon.place.repository.PlaceRepository;

/**
 * 목표: PlaceService의 비즈니스 로직(데이터 조회, DTO 변환, 예외 처리, 캐싱)이 정확하게 동작하는지 검증합니다.
 * @ExtendWith(MockitoExtension.class): Mockito 프레임워크를 사용하여 Mock 객체를 생성하고 주입할 수 있게 함
 * - PlaceRepository를 @Mock으로 만들어 DB 접근 로직과 분리
 * - 순수 유닛 테스트로 진행, 캐싱 테스트는 별도의 통합 테스트에서 다룸: @Cacheable 어노테이션의 동작을 검증하려면 @SpringBootTest를 사용하여 실제 스프링 컨텍스트와 캐시 매니저를 로드해야 하기 때문
 */
@ExtendWith(MockitoExtension.class)
public class PlaceServiceTest {

	private final GeometryFactory geometryFactory = new GeometryFactory();
	@InjectMocks
	private PlaceService placeService;
	@Mock
	private PlaceRepository placeRepository;

	@Test
	@DisplayName("장소 상세 정보 조회 및 DTO 변환 SUCCESS")
	void getPlaceDetails_and_toDto_success() {
		// given: 테스트 준비 (PlaceRepository가 특정 Place를 반환하도록 미리 stub)
		String placeId = "12345";
		Point testLocation = geometryFactory.createPoint(new Coordinate(127.123, 37.456));
		Place mockPlace = Place.builder()
			.placeId(placeId)
			.placeName("테스트 장소")
			.location(testLocation)
			.build();
		given(placeRepository.findByPlaceId(placeId)).willReturn(Optional.of(mockPlace));

		// when: 서비스의 비즈니스 메소드를 호출 시
		PlaceDetailResponse resultDto = placeService.getPlaceDetails(placeId);

		// then: 다음을 검증한다.
		assertThat(resultDto.getPlaceId()).isEqualTo(placeId); // 반환된 DTO의 값이 엔티티의 정보와 일치하는지 확인
		assertThat(resultDto.getPlaceName()).isEqualTo("테스트 장소");
		assertThat(resultDto.getLng()).isEqualTo(127.123); // X -> lng
		assertThat(resultDto.getLat()).isEqualTo(37.456);  // Y -> lat
	}

	@Test
	@DisplayName("장소 상세 정보 조회 및 DTO 변환 FAIL - 존재하지 않는 ID")
	void getPlaceDetails_fail_notFound() {
		// given: 테스트 준비
		String nonExistentPlaceId = "99999";
		given(placeRepository.findByPlaceId(nonExistentPlaceId)).willReturn(Optional.empty());

		// when & then: 서비스 메소드 호출 시 BusinessException이 발생하는지 검증한다.
		// * assertThatThrownBy 대신 assertThrows를 사용하면 발생하는 예외의 타입까지 명확하게 검증할 수 있음
		assertThrows(BusinessException.class, () -> {
			placeService.getPlaceDetails(nonExistentPlaceId);
		});
	}
}
