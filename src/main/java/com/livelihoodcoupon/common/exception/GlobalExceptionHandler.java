package com.livelihoodcoupon.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.livelihoodcoupon.common.response.CustomApiResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	// BusinessException 처리
	@ExceptionHandler(BusinessException.class)
	protected ResponseEntity<CustomApiResponse<?>> handleBusinessException(BusinessException e) {
		log.error("BusinessException: {}", e.getMessage(), e);
		return ResponseEntity
			.status(e.getErrorCode().getStatus())
			.body(CustomApiResponse.error(e.getErrorCode(), e.getMessage()));
	}

	// @Valid 또는 @Validated 에 의한 유효성 검사 실패 시 발생
	@ExceptionHandler(MethodArgumentNotValidException.class)
	protected ResponseEntity<CustomApiResponse<?>> handleMethodArgumentNotValidException(
		MethodArgumentNotValidException e) {
		log.error("MethodArgumentNotValidException: {}", e.getMessage(), e);
		// 첫 번째 에러 메시지를 가져와서 사용
		String errorMessage = e.getBindingResult().getFieldErrors().stream()
			.map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
			.findFirst()
			.orElse(ErrorCode.INVALID_INPUT_VALUE.getMessage());

		return ResponseEntity
			.status(HttpStatus.BAD_REQUEST)
			.body(CustomApiResponse.error(ErrorCode.INVALID_INPUT_VALUE, errorMessage));
	}

	// @RequestParam 필수 파라미터 누락 시 발생
	@ExceptionHandler(MissingServletRequestParameterException.class)
	protected ResponseEntity<CustomApiResponse<?>> handleMissingServletRequestParameterException(
		MissingServletRequestParameterException e) {
		log.error("MissingServletRequestParameterException: {}", e.getMessage(), e);
		return ResponseEntity
			.status(HttpStatus.BAD_REQUEST)
			.body(CustomApiResponse.error(ErrorCode.INVALID_REQUEST_PARAM, e.getMessage()));
	}

	// @RequestParam 등에서 타입 불일치 시 발생
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	protected ResponseEntity<CustomApiResponse<?>> handleMethodArgumentTypeMismatchException(
		MethodArgumentTypeMismatchException e) {
		log.error("MethodArgumentTypeMismatchException: {}", e.getMessage(), e);
		return ResponseEntity
			.status(HttpStatus.BAD_REQUEST)
			.body(CustomApiResponse.error(ErrorCode.INVALID_REQUEST_PARAM, e.getMessage()));
	}

	// 그 외 모든 예외 처리
	@ExceptionHandler(Exception.class)
	protected ResponseEntity<CustomApiResponse<?>> handleException(Exception e) {
		log.error("Exception: {}", e.getMessage(), e);
		return ResponseEntity
			.status(HttpStatus.INTERNAL_SERVER_ERROR)
			.body(CustomApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR));
	}
}
