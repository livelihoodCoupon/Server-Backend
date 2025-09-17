package com.livelihoodcoupon.common.exception;

import org.springframework.http.HttpStatusCode;

public class KakaoApiException extends RuntimeException {
	private final HttpStatusCode statusCode;
	private final String errorBody;

	public KakaoApiException(String message, HttpStatusCode statusCode, String errorBody) {
		super(message);
		this.statusCode = statusCode;
		this.errorBody = errorBody;
	}

	public HttpStatusCode getStatusCode() {
		return statusCode;
	}

	public String getErrorBody() {
		return errorBody;
	}
}
