package com.livelihoodcoupon.route.metrics;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 길찾기 API 전용 메트릭 수집기
 * 
 * <h3>수집 메트릭:</h3>
 * <ul>
 *   <li><b>API 호출 횟수:</b> 제공자별, 경로 타입별 호출 횟수</li>
 *   <li><b>응답 시간:</b> 제공자별, 경로 타입별 평균 응답 시간</li>
 *   <li><b>성공/실패율:</b> 제공자별 성공률 및 실패 원인</li>
 *   <li><b>대체 경로 사용:</b> 대체 제공자 사용 빈도</li>
 * </ul>
 */
@Slf4j
@Component
public class RouteMetrics {

    private final MeterRegistry meterRegistry;

    // API 호출 횟수 카운터
    private final Counter kakaoApiCalls;
    private final Counter osrmApiCalls;

    // 성공/실패 카운터
    private final Counter kakaoApiSuccess;
    private final Counter kakaoApiFailure;
    private final Counter osrmApiSuccess;
    private final Counter osrmApiFailure;

    // 대체 경로 사용 카운터
    private final Counter fallbackUsage;

    // 응답 시간 타이머
    private final Timer kakaoApiTimer;
    private final Timer osrmApiTimer;

    public RouteMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // API 호출 횟수 카운터
        this.kakaoApiCalls = Counter.builder("route.api.calls")
            .tag("provider", "kakao")
            .description("카카오 API 호출 횟수")
            .register(meterRegistry);

        this.osrmApiCalls = Counter.builder("route.api.calls")
            .tag("provider", "osrm")
            .description("OSRM API 호출 횟수")
            .register(meterRegistry);

        // 성공/실패 카운터
        this.kakaoApiSuccess = Counter.builder("route.api.success")
            .tag("provider", "kakao")
            .description("카카오 API 성공 횟수")
            .register(meterRegistry);

        this.kakaoApiFailure = Counter.builder("route.api.failure")
            .tag("provider", "kakao")
            .description("카카오 API 실패 횟수")
            .register(meterRegistry);

        this.osrmApiSuccess = Counter.builder("route.api.success")
            .tag("provider", "osrm")
            .description("OSRM API 성공 횟수")
            .register(meterRegistry);

        this.osrmApiFailure = Counter.builder("route.api.failure")
            .tag("provider", "osrm")
            .description("OSRM API 실패 횟수")
            .register(meterRegistry);

        // 대체 경로 사용 카운터
        this.fallbackUsage = Counter.builder("route.fallback.usage")
            .description("대체 경로 제공자 사용 횟수")
            .register(meterRegistry);

        // 응답 시간 타이머
        this.kakaoApiTimer = Timer.builder("route.api.duration")
            .tag("provider", "kakao")
            .description("카카오 API 응답 시간")
            .register(meterRegistry);

        this.osrmApiTimer = Timer.builder("route.api.duration")
            .tag("provider", "osrm")
            .description("OSRM API 응답 시간")
            .register(meterRegistry);
    }

    /**
     * 카카오 API 호출 메트릭 기록
     */
    public void recordKakaoApiCall(boolean success, long durationMs) {
        kakaoApiCalls.increment();
        if (success) {
            kakaoApiSuccess.increment();
        } else {
            kakaoApiFailure.increment();
        }
        kakaoApiTimer.record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        
        log.debug("카카오 API 메트릭 기록: 성공={}, 응답시간={}ms", success, durationMs);
    }

    /**
     * OSRM API 호출 메트릭 기록
     */
    public void recordOsrmApiCall(boolean success, long durationMs) {
        osrmApiCalls.increment();
        if (success) {
            osrmApiSuccess.increment();
        } else {
            osrmApiFailure.increment();
        }
        osrmApiTimer.record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        
        log.debug("OSRM API 메트릭 기록: 성공={}, 응답시간={}ms", success, durationMs);
    }

    /**
     * 대체 경로 사용 메트릭 기록
     */
    public void recordFallbackUsage(String fallbackProvider) {
        Counter.builder("route.fallback.usage")
            .tag("fallback_provider", fallbackProvider)
            .register(meterRegistry)
            .increment();
        
        log.info("대체 경로 사용: {}", fallbackProvider);
    }

    /**
     * 경로 타입별 메트릭 기록
     */
    public void recordRouteTypeMetrics(String routeType, boolean success, long durationMs) {
        Counter.builder("route.requests.by_type")
            .tag("route_type", routeType)
            .tag("success", String.valueOf(success))
            .register(meterRegistry)
            .increment();

        Timer.builder("route.duration.by_type")
            .tag("route_type", routeType)
            .register(meterRegistry)
            .record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
    }
}
