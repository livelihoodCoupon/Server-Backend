package com.livelihoodcoupon.common.config;

import org.springframework.context.annotation.Bean;

public class ObjectMapper {
	@Bean
	public ObjectMapper objectMapper() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule());
		return mapper;
	}
}
