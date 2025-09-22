package com.livelihoodcoupon.common.runner;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.livelihoodcoupon.search.service.RedisWordRegister;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class RedisWordInitializer implements ApplicationRunner {

	private final RedisWordRegister redisWordRegister;

	public RedisWordInitializer(RedisWordRegister redisWordRegister) {
		this.redisWordRegister = redisWordRegister;
	}

	@Override
	public void run(ApplicationArguments args) {
		log.info("Redis 단어 등록 시작111");
		redisWordRegister.wordRegister();
		log.info("Redis 단어 등록 종료222");
	}
}
