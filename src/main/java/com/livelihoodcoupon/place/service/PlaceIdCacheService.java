package com.livelihoodcoupon.place.service;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

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
public class PlaceIdCacheService {

	private final PlaceRepository placeRepository;
	private Set<String> existingPlaceIds;
	private boolean isBatchMode = false;

	/**
	 * 배치 모드 활성화 - 메모리 캐시 사용
	 */
	public void enableBatchMode() {
		log.info("배치 모드 활성화 - 기존 장소 ID를 메모리 캐시에 로드 중...");
		long startTime = System.currentTimeMillis();
		existingPlaceIds = Collections.synchronizedSet(new HashSet<>(placeRepository.findAllPlaceIds()));
		long endTime = System.currentTimeMillis();
		log.info("{}개의 기존 장소 ID 로드 완료. 소요 시간: {} ms.", existingPlaceIds.size(), (endTime - startTime));
		isBatchMode = true;
	}

	/**
	 * 배치 모드 비활성화 - Redis 캐시 사용
	 */
	public void disableBatchMode() {
		log.info("배치 모드 비활성화 - 메모리 캐시 해제");
		existingPlaceIds = null;
		isBatchMode = false;
	}

	/**
	 * 장소 ID 존재 여부를 확인합니다.
	 * 배치 모드일 때는 메모리 캐시를, 일반 모드일 때는 Redis 캐시를 사용합니다.
	 */
	@Transactional(readOnly = true)
	public boolean contains(String placeId) {
		if (isBatchMode && existingPlaceIds != null) {
			// 배치 모드: 메모리 캐시 사용 (빠름)
			log.debug("배치 모드 - 메모리 캐시에서 장소 ID 확인: {}", placeId);
			return existingPlaceIds.contains(placeId);
		} else {
			// 일반 모드: Redis 캐시 사용
			log.debug("일반 모드 - Redis 캐시에서 장소 ID 확인: {}", placeId);
			return containsWithRedisCache(placeId);
		}
	}

	/**
	 * Redis 캐시를 사용하여 장소 ID 존재 여부를 확인합니다.
	 */
	@Cacheable(value = "placeIds", key = "#placeId", unless = "#result == false")
	@Transactional(readOnly = true)
	public boolean containsWithRedisCache(String placeId) {
		log.debug("Redis 캐시에서 장소 ID 존재 여부 확인: {}", placeId);
		boolean exists = placeRepository.existsByPlaceId(placeId);
		log.debug("장소 ID {} 존재 여부: {}", placeId, exists);
		return exists;
	}

	/**
	 * 새로운 장소 ID를 캐시에 추가합니다.
	 */
	@CachePut(value = "placeIds", key = "#placeId")
	public boolean add(String placeId) {
		if (isBatchMode && existingPlaceIds != null) {
			// 배치 모드: 메모리 캐시에 추가
			log.debug("배치 모드 - 메모리 캐시에 장소 ID 추가: {}", placeId);
			existingPlaceIds.add(placeId);
			return true;
		} else {
			// 일반 모드: Redis 캐시에 추가
			log.debug("일반 모드 - Redis 캐시에 장소 ID 추가: {}", placeId);
			return true; // Redis에 true 값으로 저장
		}
	}
}
