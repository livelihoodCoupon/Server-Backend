package com.livelihoodcoupon.place.controller;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
}
