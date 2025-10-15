package com.livelihoodcoupon.common.runner;

import java.io.IOException;

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
	public void run(ApplicationArguments args) throws IOException {
		log.info("Redis 단어 등록 시작");
		redisWordRegister.fileWordRegister("address");
		redisWordRegister.fileWordRegister("category");
		log.info("Redis 단어 등록 종료");
	}
}
