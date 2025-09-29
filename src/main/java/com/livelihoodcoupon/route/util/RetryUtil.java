package com.livelihoodcoupon.route.util;

import java.util.Objects;
import java.util.function.Supplier;

import lombok.extern.slf4j.Slf4j;

/**
 * 재시도 로직을 위한 유틸리티 클래스
 *
 * <p>외부 API 호출 시 일시적인 네트워크 오류나 서버 오류에 대한 재시도 로직을 제공합니다.</p>
 */
@Slf4j
public class RetryUtil {

	private static final int DEFAULT_MAX_ATTEMPTS = 3;
	private static final long DEFAULT_DELAY_MS = 1000; // 1초

	/**
	 * 기본 설정으로 재시도를 수행합니다.
	 *
	 * @param operation 재시도할 작업
	 * @param <T> 반환 타입
	 * @return 작업 결과
	 * @throws Exception 모든 재시도가 실패한 경우
	 */
	public static <T> T retry(Supplier<T> operation) throws Exception {
		return retry(operation, DEFAULT_MAX_ATTEMPTS, DEFAULT_DELAY_MS);
	}

	/**
	 * 지정된 설정으로 재시도를 수행합니다.
	 *
	 * @param operation 재시도할 작업
	 * @param maxAttempts 최대 시도 횟수
	 * @param delayMs 재시도 간격 (밀리초)
	 * @param <T> 반환 타입
	 * @return 작업 결과
	 * @throws Exception 모든 재시도가 실패한 경우
	 */
	public static <T> T retry(Supplier<T> operation, int maxAttempts, long delayMs) throws Exception {
		Exception lastException = null;

		for (int attempt = 1; attempt <= maxAttempts; attempt++) {
			try {
				log.debug("재시도 시도 {}/{}", attempt, maxAttempts);
				return operation.get();
			} catch (Exception e) {
				lastException = e;
				log.warn("재시도 시도 {}/{} 실패: {}", attempt, maxAttempts, e.getMessage());

				if (attempt < maxAttempts) {
					try {
						Thread.sleep(delayMs);
					} catch (InterruptedException ie) {
						Thread.currentThread().interrupt();
						throw new RuntimeException("재시도 중 인터럽트됨", ie);
					}
				}
			}
		}

		log.error("모든 재시도 시도 실패 ({}회)", maxAttempts);
		throw Objects.requireNonNull(lastException);
	}

	/**
	 * 재시도 가능한 예외인지 확인합니다.
	 *
	 * @param exception 확인할 예외
	 * @return 재시도 가능 여부
	 */
	public static boolean isRetryableException(Exception exception) {
		if (exception == null) {
			return false;
		}

		String message = exception.getMessage();
		if (message == null) {
			return false;
		}

		// 네트워크 관련 오류는 재시도 가능
		return message.contains("timeout")
			|| message.contains("connection")
			|| message.contains("network")
			|| message.contains("temporary")
			|| message.contains("503")
			|| message.contains("502")
			|| message.contains("504");
	}
}
