package com.livelihoodcoupon.common.config;

import static org.mockito.Mockito.*;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import kr.co.shineware.nlp.komoran.core.Komoran;

@TestConfiguration
public class TestConfig {
	@Bean
	public Komoran komoran() {
		return mock(Komoran.class);
	}
}
