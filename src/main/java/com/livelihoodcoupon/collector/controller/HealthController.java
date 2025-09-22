package com.livelihoodcoupon.collector.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.livelihoodcoupon.common.response.CustomApiResponse;

import lombok.RequiredArgsConstructor;

/**
 * 애플리케이션 상태 확인을 위한 Health Check 컨트롤러
 *
 * <h3>주요 기능:</h3>
 * <ul>
 *   <li><b>애플리케이션 상태 확인:</b> 서비스가 정상적으로 실행 중인지 확인</li>
 *   <li><b>기본 헬스 체크:</b> 간단한 상태 정보 제공</li>
 * </ul>
 *
 * @author livelihoodCoupon Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class HealthController {

	/**
	 * 애플리케이션 상태 확인
	 *
	 * <p>애플리케이션이 정상적으로 실행 중인지 확인하는 기본 헬스 체크 엔드포인트입니다.</p>
	 *
	 * @return 애플리케이션 상태 정보
	 */
	@GetMapping("/health")
	public ResponseEntity<CustomApiResponse<String>> health() {
		return ResponseEntity.ok(
			CustomApiResponse.success("애플리케이션이 정상적으로 실행 중입니다.")
		);
	}
}
