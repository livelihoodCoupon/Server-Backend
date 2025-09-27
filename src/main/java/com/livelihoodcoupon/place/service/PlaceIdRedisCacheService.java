package com.livelihoodcoupon.place.service;

import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.livelihoodcoupon.place.repository.PlaceRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Profile("!test")
public class PlaceIdRedisCacheService {

	private final PlaceRepository placeRepository;

	/**
	 * Redis 캐시를 사용하여 장소 ID 존재 여부를 확인합니다.
	 */
	@Cacheable(value = "placeIds", key = "#placeId", unless = "#result == false")
	@Transactional(readOnly = true)
	public boolean contains(String placeId) {
		log.debug("Redis 캐시에서 장소 ID 존재 여부 확인: {}", placeId);
		boolean exists = placeRepository.existsByPlaceId(placeId);
		log.debug("장소 ID {} 존재 여부: {}", placeId, exists);
		return exists;
	}

	/**
	 * 새로운 장소 ID를 Redis 캐시에 추가합니다.
	 */
	@CachePut(value = "placeIds", key = "#placeId")
	public boolean add(String placeId) {
		log.debug("Redis 캐시에 장소 ID 추가: {}", placeId);
		return true; // Redis에 true 값으로 저장
	}
}
