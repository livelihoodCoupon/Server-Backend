package com.livelihoodcoupon.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * OffsetDateTime 매핑할때 사용
 */
@Configuration
public class ObjectMapperConfig {
	@Bean
	public ObjectMapper objectMapper() {
		ObjectMapper objectMapper = new ObjectMapper();
		// Java 8 날짜/시간 모듈 등록
		objectMapper.registerModule(new JavaTimeModule());
		// ISO 8601 형식으로 출력
		objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		return objectMapper;
	}
}
