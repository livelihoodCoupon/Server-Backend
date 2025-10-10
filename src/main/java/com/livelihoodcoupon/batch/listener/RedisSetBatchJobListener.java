package com.livelihoodcoupon.batch.listener;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisSetBatchJobListener implements JobExecutionListener {

	public static final String REDIS_TEMP_SET_KEY = "redisTempSetKey";
	private final StringRedisTemplate stringRedisTemplate;

	@Override
	public void beforeJob(JobExecution jobExecution) {
		log.info("BATCH_JOB_START: Redis 사전 작업 없음.");
	}

	@Override
	public void afterJob(JobExecution jobExecution) {
		String tempSetKey = (String)jobExecution.getExecutionContext().get(REDIS_TEMP_SET_KEY);
		if (tempSetKey != null) {
			log.info("BATCH_JOB_END: 임시 Redis SET을 삭제합니다. Key: {}", tempSetKey);
			stringRedisTemplate.delete(tempSetKey);
		}
	}
}
