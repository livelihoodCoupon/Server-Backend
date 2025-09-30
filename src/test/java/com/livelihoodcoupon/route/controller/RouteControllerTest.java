package com.livelihoodcoupon.route.controller;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.livelihoodcoupon.route.dto.RouteRequest;
import com.livelihoodcoupon.route.dto.RouteResponse;
import com.livelihoodcoupon.route.dto.RouteType;
import com.livelihoodcoupon.route.service.RouteService;

/**
 * RouteController 테스트
 */
@WebMvcTest(RouteController.class)
class RouteControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private RouteService routeService;

	@Test
	@DisplayName("길찾기 API 호출 성공")
	void getRoute_success() throws Exception {
		// given
		RouteResponse mockResponse = RouteResponse.builder()
			.routeType(RouteType.DRIVING)
			.totalDistance(1000)
			.totalDuration(300)
			.build();

		given(routeService.getRoute(any(RouteRequest.class))).willReturn(mockResponse);

		// when & then
		mockMvc.perform(get("/api/routes/search")
				.param("startLng", "127.027619")
				.param("startLat", "37.497942")
				.param("endLng", "127.028619")
				.param("endLat", "37.498942")
				.param("routeType", "driving"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.routeType").value("DRIVING"))
			.andExpect(jsonPath("$.data.totalDistance").value(1000))
			.andExpect(jsonPath("$.data.totalDuration").value(300));
	}

	@Test
	@DisplayName("지원하지 않는 경로 타입 예외")
	void getRoute_unsupportedRouteType() throws Exception {
		// given
		given(routeService.getRoute(any(RouteRequest.class)))
			.willThrow(new RuntimeException("지원하지 않는 경로 타입입니다"));

		// when & then
		mockMvc.perform(get("/api/routes/search")
				.param("startLng", "127.027619")
				.param("startLat", "37.497942")
				.param("endLng", "127.028619")
				.param("endLat", "37.498942")
				.param("routeType", "walking"))
			.andExpect(status().isInternalServerError());
	}

	@Test
	@DisplayName("제공자 목록 조회 성공")
	void getProviders_success() throws Exception {
		// given
		given(routeService.getAvailableProviders()).willReturn(List.of("KakaoNavi"));

		// when & then
		mockMvc.perform(get("/api/routes/providers"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data").isArray())
			.andExpect(jsonPath("$.data[0]").value("KakaoNavi"));
	}
}
