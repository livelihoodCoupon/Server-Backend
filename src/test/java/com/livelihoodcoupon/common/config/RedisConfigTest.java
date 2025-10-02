package com.livelihoodcoupon.common.config;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

public class RedisConfigTest {
	private final RedisConfig redisConfig = new RedisConfig();

	@Test
	@DisplayName("RedisTemplate 연결 테스트 성공")
	void redisTemplateBean_shouldBeConfiguredCorrectly() {
		// given
		RedisConnectionFactory connectionFactory = Mockito.mock(RedisConnectionFactory.class);

		// when
		RedisTemplate<String, Object> template = redisConfig.redisTemplate(connectionFactory);

		// then
		assertThat(template).isNotNull();
		assertThat(template.getConnectionFactory()).isEqualTo(connectionFactory);
		assertThat(template.getKeySerializer()).isInstanceOf(StringRedisSerializer.class);
		assertThat(template.getHashKeySerializer()).isInstanceOf(StringRedisSerializer.class);
		assertThat(template.getValueSerializer()).isInstanceOf(GenericJackson2JsonRedisSerializer.class);
		assertThat(template.getHashValueSerializer()).isInstanceOf(GenericJackson2JsonRedisSerializer.class);
	}

	@Test
	@DisplayName("CacheManager 설정 성공")
	void cacheManagerBean_shouldBeConfiguredCorrectly() {
		//given
		RedisConnectionFactory connectionFactory = Mockito.mock(RedisConnectionFactory.class);

		//when
		CacheManager cacheManager = redisConfig.cacheManager(connectionFactory);

		//then
		assertThat(cacheManager).isNotNull();
		assertThat(cacheManager).isInstanceOf(RedisCacheManager.class);
	}

}
