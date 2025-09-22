package com.livelihoodcoupon.place.controller;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.livelihoodcoupon.common.config.JpaAuditingConfig;
import com.livelihoodcoupon.common.exception.BusinessException;
import com.livelihoodcoupon.common.exception.ErrorCode;
import com.livelihoodcoupon.place.dto.PlaceDetailResponse;
import com.livelihoodcoupon.place.dto.PlaceSearchResponse;
import com.livelihoodcoupon.place.service.PlaceSearchService;
import com.livelihoodcoupon.place.service.PlaceService;

/**
 * 목표: PlaceController가 HTTP 요청을 잘 받고, 의도한 대로 PlaceService를 호출한 뒤, 정확한 HTTP 응답(상태 코드, JSON 본문)을 반환하는지 검증합니다.
 * @WebMvcTest를 사용하여 웹과 관련된 빈(Controller, Filter 등)만 로드해 웹 계층만 독립적으로 테스트
 * - PlaceService는 @MockitoBean으로 가짜 객체를 만들어, 서비스 로직과 완전히 분리
 */
@WebMvcTest(controllers = PlaceController.class,
	excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
public class PlaceControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private PlaceService placeService;

	@MockitoBean
	private PlaceSearchService placeSearchService;

	@Nested
	@DisplayName("장소 상세 정보 조회 API")
	class GetPlaceDetailsTest {
		@Test
		@DisplayName("조회 성공")
		void getPlaceDetails_success() throws Exception {
			// given: 테스트 준비 (PlaceService가 특정 DTO를 반환하도록 stub)
			String placeId = "12345";
			PlaceDetailResponse mockResponse = PlaceDetailResponse.builder()
				.placeId(placeId)
				.placeName("테스트 장소")
				.build();
			given(placeService.getPlaceDetails(placeId)).willReturn(mockResponse);

			// when: 정상적인 placeId로 장소 상세 정보 조회 api 요청을 하면
			mockMvc.perform(get("/api/places/{placeId}", placeId))
				// then: 다음을 검증한다.
				.andExpect(status().isOk()) // HTTP 상태가 200 OK
				.andExpect(jsonPath("$.success").value(true)) // 응답의 success 값은 true
				.andExpect(jsonPath("$.data.placeId").value(placeId)) // 응답의 placeId는 처음에 설정한 placeId
				.andExpect(jsonPath("$.data.placeName").value("테스트 장소")); // 응답의 placeName은 처음에 설정한 placeName
		}

		@Test
		@DisplayName("조회 실패 - 존재하지 않는 ID")
		void getPlaceDetails_fail() throws Exception {
			// given: 테스트 준비 (존재하지 않는 placeId로 조회 요청 시 PlaceService가 예외를 던지도록 미리 stub)
			String nonExistentPlaceId = "99999";
			given(placeService.getPlaceDetails(nonExistentPlaceId))
				.willThrow(new BusinessException(ErrorCode.NOT_FOUND, "장소를 찾을 수 없습니다."));

			// when: 존재하지 않는 placeId로 장소 상세 정보 조회 api 요청을 하면
			mockMvc.perform(get("/api/places/{placeId}", nonExistentPlaceId))
				// then: 다음을 검증한다.
				.andExpect(status().isNotFound()) // HTTP 상태가 404 Not Found
				.andExpect(jsonPath("$.success").value(false));
		}
	}

	@Nested
	@DisplayName("장소 반경 검색 API")
	class SearchPlacesByRadiusTest {
		@Test
		@DisplayName("검색 성공")
		void searchPlacesByRadius_success() throws Exception {
			// given: 유효한 위도, 경도, 반경으로 장소 반경 검색 요청 시 PlaceSearchService가 예상 DTO 목록을 반환하도록 stub
			double latitude = 35.876721;
			double longitude = 128.577935;
			double radiusKm = 1.0;

			List<PlaceSearchResponse> mockResponses = new ArrayList<>();
			mockResponses.add(PlaceSearchResponse.builder()
				.placeId("16406514")
				.placeName("CU 대구북비산점")
				.roadAddress("대구 서구 달성공원로 75")
				.category("가정,생활 > 편의점 > CU")
				.lat(35.876721)
				.lng(128.577935)
				.distance(0.0)
				.build());
			mockResponses.add(PlaceSearchResponse.builder()
				.placeId("16437539")
				.placeName("마라도생굴구이 인동촌점")
				.roadAddress("대구 서구 북비산로74길 50-1")
				.category("음식점 > 한식 > 해물,생선 > 굴,전복")
				.lat(35.876577)
				.lng(128.577802)
				.distance(19.98847577)
				.build());

			given(placeSearchService.searchPlacesByRadius(latitude, longitude, radiusKm)).willReturn(mockResponses);

			// when: 유효한 좌표와 반경으로 장소 반경 검색 api 요청을 하면
			mockMvc.perform(get("/api/places/search-by-radius")
					.param("latitude", String.valueOf(latitude))
					.param("longitude", String.valueOf(longitude))
					.param("radiusKm", String.valueOf(radiusKm)))
				// then: 다음을 검증한다.
				.andExpect(status().isOk()) // HTTP 상태가 200 OK
				.andExpect(jsonPath("$.success").value(true)) // 응답의 success 값은 true
				.andExpect(jsonPath("$.data.length()").value(2)) // 2개의 장소 반환
				.andExpect(jsonPath("$.data[0].placeId").value("16406514"))
				.andExpect(jsonPath("$.data[0].placeName").value("CU 대구북비산점"))
				.andExpect(jsonPath("$.data[1].placeId").value("16437539"))
				.andExpect(jsonPath("$.data[1].placeName").value("마라도생굴구이 인동촌점"));
		}

		@Test
		@DisplayName("결과 없음")
		void searchPlacesByRadius_noResults() throws Exception {
			// given: PlaceSearchService가 빈 목록을 반환하도록 stub
			double latitude = 1.0;
			double longitude = 1.0;
			double radiusKm = 1.0;

			given(placeSearchService.searchPlacesByRadius(latitude, longitude, radiusKm)).willReturn(new ArrayList<>());

			// when: 유효한 좌표와 반경으로 장소 반경 검색 api 요청을 하면
			mockMvc.perform(get("/api/places/search-by-radius")
					.param("latitude", String.valueOf(latitude))
					.param("longitude", String.valueOf(longitude))
					.param("radiusKm", String.valueOf(radiusKm)))
				// then: 다음을 검증한다.
				.andExpect(status().isOk()) // HTTP 상태가 200 OK
				.andExpect(jsonPath("$.success").value(true)) // 응답의 success 값은 true
				.andExpect(jsonPath("$.data.length()").value(0)); // 빈 목록 반환
		}

		@Test
		@DisplayName("유효하지 않은 입력")
		void searchPlacesByRadius_invalidInput() throws Exception {
			// given: PlaceSearchService가 BusinessException을 던지도록 stub
			double invalidLatitude = 999.0; // 유효하지 않은 위도
			double longitude = 128.577935;
			double radiusKm = 1.0;

			given(placeSearchService.searchPlacesByRadius(invalidLatitude, longitude, radiusKm))
				.willThrow(new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "유효하지 않은 위도 또는 경도 값입니다."));

			// when: 유효하지 않은 위도로 장소 반경 검색 api 요청을 하면
			mockMvc.perform(get("/api/places/search-by-radius")
					.param("latitude", String.valueOf(invalidLatitude))
					.param("longitude", String.valueOf(longitude))
					.param("radiusKm", String.valueOf(radiusKm)))
				// then: 다음을 검증한다.
				.andExpect(status().isBadRequest()) // HTTP 상태가 400 Bad Request
				.andExpect(jsonPath("$.success").value(false)) // 응답의 success 값은 false
				.andExpect(jsonPath("$.error.code").value(ErrorCode.INVALID_INPUT_VALUE.getCode())); // 에러 코드 검증
		}

		@Test
		@DisplayName("필수 파라미터 누락 (latitude)")
		void searchPlacesByRadius_missingLatitudeParameter() throws Exception {
			// when: 필수 파라미터(latitude) 없이 장소 반경 검색 api 요청을 하면
			mockMvc.perform(get("/api/places/search-by-radius")
					.param("longitude", "128.577935")
					.param("radiusKm", "1.0"))
				// then: 다음을 검증한다.
				.andExpect(status().isBadRequest()) // HTTP 상태가 400 Bad Request
				.andExpect(jsonPath("$.success").value(false)) // 응답의 success 값은 false
				.andExpect(jsonPath("$.error.code").value(ErrorCode.INVALID_REQUEST_PARAM.getCode())); // 에러 코드 검증
		}

		@Test
		@DisplayName("유효하지 않은 파라미터 타입 (latitude)")
		void searchPlacesByRadius_invalidLatitudeType() throws Exception {
			// when: 유효하지 않은 타입의 위도 파라미터로 장소 반경 검색 api 요청을 하면
			mockMvc.perform(get("/api/places/search-by-radius")
					.param("latitude", "not-a-number")
					.param("longitude", "128.577935")
					.param("radiusKm", "1.0"))
				// then: 다음을 검증한다.
				.andExpect(status().isBadRequest()) // HTTP 상태가 400 Bad Request
				.andExpect(jsonPath("$.success").value(false)) // 응답의 success 값은 false
				.andExpect(jsonPath("$.error.code").value(ErrorCode.INVALID_REQUEST_PARAM.getCode())); // 에러 코드 검증
		}
	}
}
