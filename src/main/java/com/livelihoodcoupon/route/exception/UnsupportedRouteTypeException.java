package com.livelihoodcoupon.route.exception;

/**
 * 지원하지 않는 경로 타입 예외
 */
public class UnsupportedRouteTypeException extends RuntimeException {

	public UnsupportedRouteTypeException(String message) {
		super(message);
	}

	public UnsupportedRouteTypeException(String message, Throwable cause) {
		super(message, cause);
	}
}
