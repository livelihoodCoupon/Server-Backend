package com.livelihoodcoupon.place.service;

import java.io.IOException;

import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Profile("!test")
public class PlaceIdEsRedisCacheService {

	private final co.elastic.clients.elasticsearch.ElasticsearchClient elasticsearchClient;

	/**
	 * Redis 캐시를 사용하여 Elasticsearch에 장소 ID 존재 여부를 확인합니다.
	 * 캐시 이름으로 "placeIds_es"를 사용합니다.
	 */
	@Cacheable(value = "placeIds_es", key = "#placeId", unless = "#result == false")
	public boolean contains(String placeId) {
		log.debug("ES Redis 캐시에서 장소 ID 존재 여부 확인: {}", placeId);
		try {
			boolean exists = elasticsearchClient.exists(r -> r
				.index("places")
				.id(placeId)
			).value();
			log.debug("ES에 장소 ID {} 존재 여부: {}", placeId, exists);
			return exists;
		} catch (IOException e) {
			log.error("Elasticsearch 'exists' 쿼리 실패: placeId={}", placeId, e);
			// 통신 오류 발생 시, 작업을 중단시키지 않고 중복이 아닌 것으로 간주하여 재처리 기회를 줍니다.
			return false;
		}
	}

	/**
	 * 새로운 장소 ID를 "placeIds_es" Redis 캐시에 추가합니다.
	 */
	@CachePut(value = "placeIds_es", key = "#placeId")
	public boolean add(String placeId) {
		log.debug("ES Redis 캐시에 장소 ID 추가: {}", placeId);
		return true; // Redis에 true 값으로 저장
	}
}
