package com.livelihoodcoupon.common.runner;

import static org.mockito.Mockito.*;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.ApplicationArguments;

import com.livelihoodcoupon.search.service.RedisWordRegister;

class RedisWordInitializerTest {

	@Mock
	private RedisWordRegister redisWordRegister;

	@InjectMocks
	private RedisWordInitializer redisWordInitializer;

	@Mock
	private ApplicationArguments args;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
	}

	@Test
	void run_shouldCallFileWordRegisterForAddressAndCategory() throws IOException {
		// when
		redisWordInitializer.run(args);

		// then
		verify(redisWordRegister).fileWordRegister("address");
		verify(redisWordRegister).fileWordRegister("category");
		verifyNoMoreInteractions(redisWordRegister);
	}
}
