package com.livelihoodcoupon.common.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter
public enum ErrorCode {

	// Common
	COMMON_BAD_REQUEST(HttpStatus.BAD_REQUEST, "C001", "Bad Request"),
	INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C002", "Invalid Input Value"),
	INVALID_REQUEST_PARAM(HttpStatus.BAD_REQUEST, "C003", "Invalid Request Parameter"),
	UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "C004", "Unauthorized"),
	FORBIDDEN(HttpStatus.FORBIDDEN, "C005", "Forbidden"),
	NOT_FOUND_URL(HttpStatus.NOT_FOUND, "C006", "Not Found URL"),
	NOT_FOUND(HttpStatus.NOT_FOUND, "C007", "Resource Not Found"),
	TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "C008", "Too Many Requests"),
	INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C009", "Internal Server Error"),
	
	// Route
	UNSUPPORTED_ROUTE_TYPE(HttpStatus.BAD_REQUEST, "R001", "지원하지 않는 경로 타입입니다"),
	ROUTE_PROVIDER_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "R002", "경로 제공자 서비스가 일시적으로 사용할 수 없습니다"),
	ROUTE_NOT_FOUND(HttpStatus.NOT_FOUND, "R003", "경로를 찾을 수 없습니다"),
	KAKAO_API_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "R004", "카카오 API 서비스 오류"),
	OSRM_API_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "R005", "OSRM 서비스 오류");

	private final HttpStatus status;
	private final String code;
	private final String message;

	ErrorCode(HttpStatus status, String code, String message) {
		this.status = status;
		this.code = code;
		this.message = message;
	}
}
