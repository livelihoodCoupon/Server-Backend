package com.livelihoodcoupon.common.runner;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.livelihoodcoupon.search.service.RedisWordRegister;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class RedisWordInitializer implements CommandLineRunner {

	private final RedisWordRegister redisWordRegister;

	public RedisWordInitializer(RedisWordRegister redisWordRegister) {
		this.redisWordRegister = redisWordRegister;
	}

	@Override
	public void run(String... args) throws Exception {
		log.info("Redis 단어 등록 시작");
		redisWordRegister.wordRegister();
		log.info("Redis 단어 등록 종료");
	}
}