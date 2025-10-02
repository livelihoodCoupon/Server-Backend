package com.livelihoodcoupon.common.config;

import java.time.Duration;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 캐싱 설정
 *
 * <h3>주요 기능:</h3>
 * <ul>
 *   <li><b>Spring Cache 활성화:</b> @Cacheable, @CachePut, @CacheEvict 사용 가능</li>
 *   <li><b>Redis 연결 설정:</b> RedisTemplate 및 CacheManager 구성</li>
 *   <li><b>JSON 직렬화:</b> 객체를 JSON으로 직렬화하여 Redis에 저장</li>
 *   <li><b>TTL 설정:</b> 캐시 만료 시간 설정</li>
 * </ul>
 */
@Configuration
@EnableCaching
public class RedisConfig {

	/**
	 * RedisTemplate 설정
	 *
	 * @param connectionFactory Redis 연결 팩토리
	 * @return RedisTemplate
	 */
	@Bean
	public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
		RedisTemplate<String, Object> template = new RedisTemplate<>();
		template.setConnectionFactory(connectionFactory);

		// Key 직렬화 설정
		template.setKeySerializer(new StringRedisSerializer());
		template.setHashKeySerializer(new StringRedisSerializer());

		// Value 직렬화 설정 (JSON)
		template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
		template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

		template.afterPropertiesSet();
		return template;
	}

	/**
	 * CacheManager 설정
	 *
	 * @param connectionFactory Redis 연결 팩토리
	 * @return CacheManager
	 */
	@Bean
	public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
		// 기본 캐시 설정
		RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
			.serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
			.serializeValuesWith(
				RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
			.entryTtl(Duration.ofMinutes(10)); // 기본 10분 TTL

		// 특정 캐시별 설정
		return RedisCacheManager.builder(connectionFactory)
			.cacheDefaults(defaultConfig)
			.withCacheConfiguration("gridCache",
				defaultConfig.entryTtl(Duration.ofHours(1))) // 격자 캐시: 1시간
			.withCacheConfiguration("placeDetails",
				defaultConfig.entryTtl(Duration.ofMinutes(30))) // 장소 상세: 30분
			.withCacheConfiguration("placeIds",
				defaultConfig.entryTtl(Duration.ofHours(2))) // 장소 ID: 2시간
			.build();
	}
}
