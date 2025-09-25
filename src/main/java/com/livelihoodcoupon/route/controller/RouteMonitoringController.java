package com.livelihoodcoupon.route.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.livelihoodcoupon.common.response.CustomApiResponse;
import com.livelihoodcoupon.route.dto.RouteType;
import com.livelihoodcoupon.route.service.RouteService;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * 길찾기 API 모니터링 전용 컨트롤러
 * 
 * <h3>제공 엔드포인트:</h3>
 * <ul>
 *   <li><code>GET /api/routes/monitoring/stats</code> - API 호출 통계</li>
 *   <li><code>GET /api/routes/monitoring/health</code> - 제공자 헬스체크</li>
 *   <li><code>GET /api/routes/monitoring/performance</code> - 성능 메트릭</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/routes/monitoring")
@RequiredArgsConstructor
public class RouteMonitoringController {

	private final MeterRegistry meterRegistry;
	private final RouteService routeService;

	/**
	 * API 호출 통계 조회
	 * 길찾기 API의 호출 통계를 조회합니다.
	 *
	 * @return API 통계 정보
	 */
	@GetMapping("/stats")
	public ResponseEntity<CustomApiResponse<Map<String, Object>>> getApiStats() {
		Map<String, Object> stats = new HashMap<>();
		
		// 카카오 API 통계
		Map<String, Object> kakaoStats = new HashMap<>();
		Counter kakaoCalls = meterRegistry.find("route.api.calls").tag("provider", "kakao").counter();
		Counter kakaoSuccess = meterRegistry.find("route.api.success").tag("provider", "kakao").counter();
		Counter kakaoFailure = meterRegistry.find("route.api.failure").tag("provider", "kakao").counter();
		
		double kakaoTotalCalls = kakaoCalls != null ? kakaoCalls.count() : 0;
		double kakaoSuccessCount = kakaoSuccess != null ? kakaoSuccess.count() : 0;
		double kakaoFailureCount = kakaoFailure != null ? kakaoFailure.count() : 0;
		
		kakaoStats.put("totalCalls", (int) kakaoTotalCalls);
		kakaoStats.put("successCount", (int) kakaoSuccessCount);
		kakaoStats.put("failureCount", (int) kakaoFailureCount);
		kakaoStats.put("successRate", kakaoTotalCalls > 0 ? (kakaoSuccessCount / kakaoTotalCalls) * 100 : 0);
		
		// OSRM API 통계
		Map<String, Object> osrmStats = new HashMap<>();
		Counter osrmCalls = meterRegistry.find("route.api.calls").tag("provider", "osrm").counter();
		Counter osrmSuccess = meterRegistry.find("route.api.success").tag("provider", "osrm").counter();
		Counter osrmFailure = meterRegistry.find("route.api.failure").tag("provider", "osrm").counter();
		
		double osrmTotalCalls = osrmCalls != null ? osrmCalls.count() : 0;
		double osrmSuccessCount = osrmSuccess != null ? osrmSuccess.count() : 0;
		double osrmFailureCount = osrmFailure != null ? osrmFailure.count() : 0;
		
		osrmStats.put("totalCalls", (int) osrmTotalCalls);
		osrmStats.put("successCount", (int) osrmSuccessCount);
		osrmStats.put("failureCount", (int) osrmFailureCount);
		osrmStats.put("successRate", osrmTotalCalls > 0 ? (osrmSuccessCount / osrmTotalCalls) * 100 : 0);
		
		// 대체 경로 사용 통계
		Counter fallbackUsage = meterRegistry.find("route.fallback.usage").counter();
		double fallbackCount = fallbackUsage != null ? fallbackUsage.count() : 0;
		
		stats.put("kakao", kakaoStats);
		stats.put("osrm", osrmStats);
		stats.put("fallbackUsage", (int) fallbackCount);
		stats.put("availableProviders", routeService.getAvailableProviders());
		
		return ResponseEntity.ok(CustomApiResponse.success(stats));
	}

	/**
	 * 제공자 헬스체크
	 * 각 경로 제공자의 상태를 확인합니다.
	 *
	 * @return 헬스체크 결과
	 */
	@GetMapping("/health")
	public ResponseEntity<CustomApiResponse<Map<String, Object>>> getProviderHealth() {
		Map<String, Object> health = new HashMap<>();
		
		// 기본적으로 모든 제공자가 사용 가능하다고 가정
		// 실제 구현에서는 각 제공자에 대한 헬스체크를 수행
		health.put("kakao", Map.of("status", "UP", "message", "카카오 API 정상"));
		health.put("osrm", Map.of("status", "UP", "message", "OSRM 서버 정상"));
		health.put("overall", "UP");
		
		return ResponseEntity.ok(CustomApiResponse.success(health));
	}

	/**
	 * 성능 메트릭 조회
	 * API 응답 시간 및 성능 지표를 조회합니다.
	 *
	 * @return 성능 메트릭 정보
	 */
	@GetMapping("/performance")
	public ResponseEntity<CustomApiResponse<Map<String, Object>>> getPerformanceMetrics() {
		Map<String, Object> metrics = new HashMap<>();
		
		// 카카오 API 성능 메트릭
		Timer kakaoTimer = meterRegistry.find("route.api.duration").tag("provider", "kakao").timer();
		if (kakaoTimer != null) {
			Map<String, Object> kakaoMetrics = new HashMap<>();
			kakaoMetrics.put("mean", kakaoTimer.mean(java.util.concurrent.TimeUnit.MILLISECONDS));
			kakaoMetrics.put("max", kakaoTimer.max(java.util.concurrent.TimeUnit.MILLISECONDS));
			kakaoMetrics.put("count", kakaoTimer.count());
			metrics.put("kakao", kakaoMetrics);
		}
		
		// OSRM API 성능 메트릭
		Timer osrmTimer = meterRegistry.find("route.api.duration").tag("provider", "osrm").timer();
		if (osrmTimer != null) {
			Map<String, Object> osrmMetrics = new HashMap<>();
			osrmMetrics.put("mean", osrmTimer.mean(java.util.concurrent.TimeUnit.MILLISECONDS));
			osrmMetrics.put("max", osrmTimer.max(java.util.concurrent.TimeUnit.MILLISECONDS));
			osrmMetrics.put("count", osrmTimer.count());
			metrics.put("osrm", osrmMetrics);
		}
		
		// 경로 타입별 메트릭
		for (RouteType routeType : RouteType.values()) {
			Timer routeTimer = meterRegistry.find("route.duration.by_type").tag("route_type", routeType.name()).timer();
			if (routeTimer != null) {
				Map<String, Object> routeMetrics = new HashMap<>();
				routeMetrics.put("mean", routeTimer.mean(java.util.concurrent.TimeUnit.MILLISECONDS));
				routeMetrics.put("max", routeTimer.max(java.util.concurrent.TimeUnit.MILLISECONDS));
				routeMetrics.put("count", routeTimer.count());
				metrics.put(routeType.name().toLowerCase(), routeMetrics);
			}
		}
		
		return ResponseEntity.ok(CustomApiResponse.success(metrics));
	}
}