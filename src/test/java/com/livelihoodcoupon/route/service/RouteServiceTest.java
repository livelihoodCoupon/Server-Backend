package com.livelihoodcoupon.route.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.livelihoodcoupon.route.dto.RouteRequest;
import com.livelihoodcoupon.route.dto.RouteResponse;
import com.livelihoodcoupon.route.dto.RouteType;
import com.livelihoodcoupon.route.logging.RouteLoggingService;
import com.livelihoodcoupon.route.metrics.RouteMetrics;

/**
 * RouteService 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
class RouteServiceTest {
    
    @Mock
    private KakaoRouteProvider mockKakaoProvider;
    
    @Mock
    private OsrmRouteProvider mockOsrmProvider;
    
    @Mock
    private RouteMetrics mockRouteMetrics;
    
    @Mock
    private RouteLoggingService mockRouteLoggingService;
    
    private RouteService routeService;
    
    @Test
    @DisplayName("자동차 경로 조회 성공")
    void getDrivingRoute_success() {
        // given
        routeService = new RouteService(mockKakaoProvider, mockOsrmProvider, mockRouteMetrics, mockRouteLoggingService);
        
        RouteRequest request = RouteRequest.builder()
            .startLng(127.027619)
            .startLat(37.497942)
            .endLng(127.028619)
            .endLat(37.498942)
            .routeType(RouteType.DRIVING)
            .build();
            
        RouteResponse mockResponse = RouteResponse.builder()
            .routeType(RouteType.DRIVING)
            .totalDistance(1000)
            .totalDuration(300)
            .build();
        
        // when
        when(mockKakaoProvider.getRoute(request)).thenReturn(mockResponse);
        
        RouteResponse response = routeService.getRoute(request);
        
        // then
        assertThat(response).isNotNull();
        assertThat(response.getRouteType()).isEqualTo(RouteType.DRIVING);
        assertThat(response.getTotalDistance()).isEqualTo(1000);
        assertThat(response.getTotalDuration()).isEqualTo(300);
    }
    
    @Test
    @DisplayName("지원하지 않는 경로 타입 예외 발생")
    void getUnsupportedRouteType_throwsException() {
        // given
        routeService = new RouteService(mockKakaoProvider, mockOsrmProvider, mockRouteMetrics, mockRouteLoggingService);
        
        // when: 지원하지 않는 경로 타입을 테스트
        RouteRequest invalidRequest = RouteRequest.builder()
            .startLng(127.027619)
            .startLat(37.497942)
            .endLng(127.028619)
            .endLat(37.498942)
            .routeType(null) // null 경로 타입으로 예외 발생
            .build();
        
        // then
        assertThatThrownBy(() -> routeService.getRoute(invalidRequest))
            .isInstanceOf(com.livelihoodcoupon.common.exception.BusinessException.class)
            .hasMessageContaining("경로 타입이 지정되지 않았습니다");
    }
    
    @Test
    @DisplayName("사용 가능한 제공자 목록 조회")
    void getAvailableProviders_success() {
        // given
        routeService = new RouteService(mockKakaoProvider, mockOsrmProvider, mockRouteMetrics, mockRouteLoggingService);
        when(mockKakaoProvider.getProviderName()).thenReturn("KakaoNavi");
        when(mockOsrmProvider.getProviderName()).thenReturn("OSRM");
        
        // when
        List<String> providers = routeService.getAvailableProviders();
        
        // then
        assertThat(providers).isNotEmpty();
        assertThat(providers).contains("KakaoNavi", "OSRM");
    }
}
