package com.livelihoodcoupon.route.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.livelihoodcoupon.common.exception.BusinessException;
import com.livelihoodcoupon.common.exception.ErrorCode;
import com.livelihoodcoupon.route.dto.RouteRequest;
import com.livelihoodcoupon.route.dto.RouteResponse;
import com.livelihoodcoupon.route.dto.RouteType;
import com.livelihoodcoupon.route.logging.RouteLoggingService;
import com.livelihoodcoupon.route.metrics.RouteMetrics;
import com.livelihoodcoupon.route.util.RetryUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * 통합 경로 서비스
 * 다양한 경로 제공자를 통합하여 관리하는 서비스
 */
@Slf4j
@Service
public class RouteService {

	private final KakaoRouteProvider kakaoRouteProvider;
	private final OsrmRouteProvider osrmRouteProvider;
	private final RouteMetrics routeMetrics;
	private final RouteLoggingService routeLoggingService;

	@Autowired
	public RouteService(KakaoRouteProvider kakaoRouteProvider, OsrmRouteProvider osrmRouteProvider, 
			RouteMetrics routeMetrics, RouteLoggingService routeLoggingService) {
		this.kakaoRouteProvider = kakaoRouteProvider;
		this.osrmRouteProvider = osrmRouteProvider;
		this.routeMetrics = routeMetrics;
		this.routeLoggingService = routeLoggingService;
	}

	/**
	 * 경로를 조회합니다.
	 * 경로 타입에 따라 적절한 제공자를 선택하여 경로를 조회합니다.
	 * 주요 제공자 실패 시 대체 제공자를 시도합니다.
	 *
	 * @param request 경로 요청 정보
	 * @return 경로 응답 정보
	 * @throws BusinessException 지원하지 않는 경로 타입이거나 경로 제공자 실패인 경우
	 */
	@Cacheable(value = "routes", key = "#request.toString()")
	public RouteResponse getRoute(RouteRequest request) {
		long startTime = System.currentTimeMillis();
		RouteProvider primaryProvider = selectProvider(request.getRouteType());
		
		// 요청 로그 기록
		routeLoggingService.logRouteRequest(request, primaryProvider.getProviderName());
		
		try {
			// 재시도 로직과 함께 주요 제공자로 경로 조회
			RouteResponse response = RetryUtil.retry(() -> primaryProvider.getRoute(request));
			
			// 성공 메트릭 및 로그 기록
			recordMetrics(primaryProvider, true, startTime, request.getRouteType().name());
			routeLoggingService.logRouteResponse(response, primaryProvider.getProviderName(), 
				System.currentTimeMillis() - startTime);
			
			return response;
			
		} catch (Exception e) {
			log.warn("주요 제공자 실패, 대체 경로 시도: {} - {}", primaryProvider.getProviderName(), e.getMessage());
			
			// 주요 제공자 실패 메트릭 및 로그 기록
			recordMetrics(primaryProvider, false, startTime, request.getRouteType().name());
			routeLoggingService.logApiFailure(primaryProvider.getProviderName(), e.getMessage(), 
				System.currentTimeMillis() - startTime, 3); // RetryUtil의 MAX_RETRIES
			
			// 대체 경로 제공자 시도
			RouteProvider fallbackProvider = selectFallbackProvider(request.getRouteType());
			if (fallbackProvider != null) {
				try {
					log.info("대체 제공자로 경로 조회 시도: {}", fallbackProvider.getProviderName());
					RouteResponse response = fallbackProvider.getRoute(request);
					
					// 대체 제공자 성공 메트릭 및 로그 기록
					recordMetrics(fallbackProvider, true, startTime, request.getRouteType().name());
					routeMetrics.recordFallbackUsage(fallbackProvider.getProviderName());
					routeLoggingService.logFallbackUsage(primaryProvider.getProviderName(), 
						fallbackProvider.getProviderName(), e.getMessage());
					routeLoggingService.logRouteResponse(response, fallbackProvider.getProviderName(), 
						System.currentTimeMillis() - startTime);
					
					return response;
				} catch (Exception fallbackException) {
					log.error("대체 제공자도 실패: {} - {}", fallbackProvider.getProviderName(), fallbackException.getMessage());
					
					// 대체 제공자 실패 메트릭 및 로그 기록
					recordMetrics(fallbackProvider, false, startTime, request.getRouteType().name());
					routeLoggingService.logApiFailure(fallbackProvider.getProviderName(), fallbackException.getMessage(), 
						System.currentTimeMillis() - startTime, 0);
					
					throw new BusinessException(ErrorCode.ROUTE_PROVIDER_FAILED, 
						"모든 경로 제공자가 실패했습니다. 잠시 후 다시 시도해주세요.");
				}
			} else {
				throw new BusinessException(ErrorCode.ROUTE_PROVIDER_FAILED, 
					"경로 제공자 서비스가 일시적으로 사용할 수 없습니다. 잠시 후 다시 시도해주세요.");
			}
		}
	}

	/**
	 * 경로 타입에 따라 적절한 제공자를 선택합니다.
	 *
	 * @param routeType 경로 타입
	 * @return 선택된 제공자
	 * @throws BusinessException 지원하지 않는 경로 타입이거나 경로 제공자 실패인 경우
	 */
	private RouteProvider selectProvider(RouteType routeType) {
		if (routeType == null) {
			throw new BusinessException(ErrorCode.UNSUPPORTED_ROUTE_TYPE, 
				"경로 타입이 지정되지 않았습니다. DRIVING, WALKING, CYCLING, TRANSIT 중 하나를 선택해주세요.");
		}
		
		switch (routeType) {
			case DRIVING:
				return kakaoRouteProvider;
			case WALKING:
			case CYCLING:
			case TRANSIT:
				return osrmRouteProvider;
			default:
				throw new BusinessException(ErrorCode.UNSUPPORTED_ROUTE_TYPE, 
					"지원하지 않는 경로 타입입니다: " + routeType + ". DRIVING, WALKING, CYCLING, TRANSIT 중 하나를 선택해주세요.");
		}
	}

	/**
	 * 대체 경로 제공자를 선택합니다.
	 * 
	 * @param routeType 경로 타입
	 * @return 대체 제공자 (없으면 null)
	 */
	private RouteProvider selectFallbackProvider(RouteType routeType) {
		switch (routeType) {
			case DRIVING:
				// 카카오 API 실패 시 OSRM으로 대체 (자동차 경로)
				log.info("카카오 API 실패, OSRM으로 대체 시도");
				return osrmRouteProvider;
			case WALKING:
			case CYCLING:
			case TRANSIT:
				// OSRM 실패 시 카카오 API로 대체 (도보 경로로)
				log.info("OSRM 실패, 카카오 API로 대체 시도");
				return kakaoRouteProvider;
			default:
				return null;
		}
	}

	/**
	 * 사용 가능한 경로 제공자 목록을 반환합니다.
	 *
	 * @return 제공자 이름 목록
	 */
	public List<String> getAvailableProviders() {
		return List.of(
			kakaoRouteProvider.getProviderName(),
			osrmRouteProvider.getProviderName()
		);
	}

	/**
	 * 제공자별 메트릭을 기록합니다.
	 *
	 * @param provider 경로 제공자
	 * @param success 성공 여부
	 * @param startTime 시작 시간
	 * @param routeType 경로 타입
	 */
	private void recordMetrics(RouteProvider provider, boolean success, long startTime, String routeType) {
		long duration = System.currentTimeMillis() - startTime;
		
		if (provider instanceof KakaoRouteProvider) {
			routeMetrics.recordKakaoApiCall(success, duration);
		} else if (provider instanceof OsrmRouteProvider) {
			routeMetrics.recordOsrmApiCall(success, duration);
		}
		
		// 경로 타입별 메트릭 기록
		routeMetrics.recordRouteTypeMetrics(routeType, success, duration);
	}
}
