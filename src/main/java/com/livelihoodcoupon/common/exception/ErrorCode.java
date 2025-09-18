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
	INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C009", "Internal Server Error");

	private final HttpStatus status;
	private final String code;
	private final String message;

	ErrorCode(HttpStatus status, String code, String message) {
		this.status = status;
		this.code = code;
		this.message = message;
	}
}
