package com.livelihoodcoupon.place.service;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.livelihoodcoupon.common.exception.BusinessException;
import com.livelihoodcoupon.place.dto.PlaceSearchResponse;
import com.livelihoodcoupon.place.repository.PlaceRepository;
import com.livelihoodcoupon.place.repository.PlaceWithDistance;

@ExtendWith(MockitoExtension.class)
class PlaceSearchServiceTest {

	@InjectMocks
	private PlaceSearchServiceImpl placeSearchService;

	@Mock
	private PlaceRepository placeRepository;

	@Test
	@DisplayName("유효한 값으로 장소 검색시 성공")
	void searchPlacesByRadius_withValidInput_shouldReturnListOfPlaces() {
		// given
		double latitude = 37.5665;
		double longitude = 126.9780;
		double radiusKm = 1.0;
		PlaceWithDistance placeWithDistance = new PlaceWithDistance() {
			public Long getId() {
				return 1L;
			}

			public String getPlaceId() {
				return "place1";
			}

			public String getRegion() {
				return "서울";
			}

			public String getPlaceName() {
				return "test place";
			}

			public String getRoadAddress() {
				return "test road address";
			}

			public String getLotAddress() {
				return "test lot address";
			}

			public String getPhone() {
				return "010-1234-5678";
			}

			public String getCategory() {
				return "test category";
			}

			public String getKeyword() {
				return "test keyword";
			}

			public String getCategoryGroupCode() {
				return "test category group code";
			}

			public String getCategoryGroupName() {
				return "test category group name";
			}

			public String getPlaceUrl() {
				return "test place url";
			}

			public Double getLat() {
				return 37.5665;
			}

			public Double getLng() {
				return 126.9780;
			}

			public OffsetDateTime getCreatedAt() {
				return OffsetDateTime.now();
			}

			public OffsetDateTime getUpdatedAt() {
				return OffsetDateTime.now();
			}

			public Double getDistance() {
				return 500.0;
			}
		};
		when(placeRepository.findPlacesWithinRadius(anyDouble(), anyDouble(), anyDouble()))
			.thenReturn(List.of(placeWithDistance));

		// when
		List<PlaceSearchResponse> result = placeSearchService.searchPlacesByRadius(latitude, longitude, radiusKm);

		// then
		assertThat(result).hasSize(1);
		assertThat(result.get(0).getPlaceId()).isEqualTo("place1");
	}

	@Test
	@DisplayName("잘못된 위도 값으로 장소 검색시 예외 발생")
	void searchPlacesByRadius_withInvalidLatitude_shouldThrowException() {
		// given
		double latitude = 91.0;
		double longitude = 126.9780;
		double radiusKm = 1.0;

		// when & then
		assertThrows(BusinessException.class, () -> {
			placeSearchService.searchPlacesByRadius(latitude, longitude, radiusKm);
		});
	}

	@Test
	@DisplayName("잘못된 경도 값으로 장소 검색시 예외 발생")
	void searchPlacesByRadius_withInvalidLongitude_shouldThrowException() {
		// given
		double latitude = 37.5665;
		double longitude = 181.0;
		double radiusKm = 1.0;

		// when & then
		assertThrows(BusinessException.class, () -> {
			placeSearchService.searchPlacesByRadius(latitude, longitude, radiusKm);
		});
	}

	@Test
	@DisplayName("잘못된 반경 값으로 장소 검색시 예외 발생")
	void searchPlacesByRadius_withInvalidRadius_shouldThrowException() {
		// given
		double latitude = 37.5665;
		double longitude = 126.9780;
		double radiusKm = 0.0;

		// when & then
		assertThrows(BusinessException.class, () -> {
			placeSearchService.searchPlacesByRadius(latitude, longitude, radiusKm);
		});
	}

	@Test
	@DisplayName("검색 결과가 없을 경우 빈 리스트 반환")
	void searchPlacesByRadius_withNoResults_shouldReturnEmptyList() {
		// given
		double latitude = 37.5665;
		double longitude = 126.9780;
		double radiusKm = 1.0;
		when(placeRepository.findPlacesWithinRadius(anyDouble(), anyDouble(), anyDouble()))
			.thenReturn(Collections.emptyList());

		// when
		List<PlaceSearchResponse> result = placeSearchService.searchPlacesByRadius(latitude, longitude, radiusKm);

		// then
		assertThat(result).isEmpty();
	}
}
