package com.livelihoodcoupon.common.filter;

import java.io.IOException;
import java.util.UUID;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class LoggingFilter implements Filter {

	/**
	 * MDC-CONTEXT:
	 * - 공통 필드: traceId
	 */
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
		throws IOException, ServletException {
		try {
			// 1. traceId 생성 및 MDC에 추가
			String traceId = UUID.randomUUID().toString();
			MDC.put("traceId", traceId);

			// 2. Spring Security의 SecurityContext에서 인증 정보 추출 (제거됨)
			// Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

			// 인증된 사용자인 경우 memberId와 memberRole을 MDC에 추가 (제거됨)
			// if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(
			// authentication.getPrincipal())) {
			// // Principal의 name을 memberId로 사용합니다. (이메일)
			// MDC.put("memberId", authentication.getName());
			//
			// // 사용자의 첫 번째 권한(Role)을 memberRole로 MDC에 추가
			// authentication.getAuthorities().stream()
			// .findFirst()
			// .map(GrantedAuthority::getAuthority)
			// .map(role -> role.startsWith("ROLE_") ? role.substring(5) : role) // "ROLE_" 접두사 제거
			// .ifPresent(role -> MDC.put("memberRole", role));
			// }

			chain.doFilter(request, response);
		} finally {
			// 3. 요청 처리가 끝난 후 MDC의 모든 정보 제거
			MDC.clear();
		}
	}
}