package com.livelihoodcoupon.route.logging;

import org.springframework.stereotype.Service;

import com.livelihoodcoupon.route.dto.RouteRequest;
import com.livelihoodcoupon.route.dto.RouteResponse;
import com.livelihoodcoupon.route.dto.RouteType;

import lombok.extern.slf4j.Slf4j;

/**
 * 길찾기 API 전용 로깅 서비스
 * 
 * <h3>로깅 정보:</h3>
 * <ul>
 *   <li><b>요청 정보:</b> 출발지, 도착지, 경로 타입</li>
 *   <li><b>응답 정보:</b> 거리, 소요시간, 좌표 수</li>
 *   <li><b>성능 정보:</b> 응답 시간, 제공자 정보</li>
 *   <li><b>에러 정보:</b> 실패 원인, 재시도 횟수</li>
 * </ul>
 */
@Slf4j
@Service
public class RouteLoggingService {

    /**
     * 경로 요청 로그를 기록합니다.
     */
    public void logRouteRequest(RouteRequest request, String providerName) {
        log.info("경로 요청 시작 - 제공자: {}, 타입: {}, 출발지: ({}, {}), 도착지: ({}, {})",
            providerName,
            request.getRouteType(),
            request.getStartLng(), request.getStartLat(),
            request.getEndLng(), request.getEndLat());
    }

    /**
     * 경로 응답 로그를 기록합니다.
     */
    public void logRouteResponse(RouteResponse response, String providerName, long durationMs) {
        log.info("경로 응답 완료 - 제공자: {}, 타입: {}, 거리: {}m, 소요시간: {}초, 좌표수: {}, 응답시간: {}ms",
            providerName,
            response.getRouteType(),
            Math.round(response.getTotalDistance()),
            Math.round(response.getTotalDuration()),
            response.getCoordinates() != null ? response.getCoordinates().size() : 0,
            durationMs);
    }

    /**
     * API 실패 로그를 기록합니다.
     */
    public void logApiFailure(String providerName, String errorMessage, long durationMs, int retryCount) {
        log.warn("API 호출 실패 - 제공자: {}, 오류: {}, 응답시간: {}ms, 재시도: {}회",
            providerName, errorMessage, durationMs, retryCount);
    }

    /**
     * 대체 경로 사용 로그를 기록합니다.
     */
    public void logFallbackUsage(String primaryProvider, String fallbackProvider, String reason) {
        log.info("대체 경로 사용 - 주요: {}, 대체: {}, 사유: {}", 
            primaryProvider, fallbackProvider, reason);
    }

    /**
     * 캐시 히트/미스 로그를 기록합니다.
     */
    public void logCacheEvent(String cacheKey, boolean hit) {
        if (hit) {
            log.debug("캐시 히트 - 키: {}", cacheKey);
        } else {
            log.debug("캐시 미스 - 키: {}", cacheKey);
        }
    }

    /**
     * 경로 타입별 통계 로그를 기록합니다.
     */
    public void logRouteTypeStats(RouteType routeType, long totalRequests, long successCount, double avgResponseTime) {
        double successRate = (double) successCount / totalRequests * 100;
        log.info("경로 타입 통계 - 타입: {}, 총요청: {}, 성공률: {:.1f}%, 평균응답시간: {:.0f}ms",
            routeType, totalRequests, successRate, avgResponseTime);
    }

    /**
     * 제공자별 성능 비교 로그를 기록합니다.
     */
    public void logProviderComparison(String provider1, double avgTime1, String provider2, double avgTime2) {
        log.info("제공자 성능 비교 - {}: {:.0f}ms, {}: {:.0f}ms, 차이: {:.0f}ms",
            provider1, avgTime1, provider2, avgTime2, Math.abs(avgTime1 - avgTime2));
    }
}
