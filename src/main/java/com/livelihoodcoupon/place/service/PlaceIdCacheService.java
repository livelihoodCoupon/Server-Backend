package com.livelihoodcoupon.place.service;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import jakarta.annotation.PostConstruct;

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

	@PostConstruct
	@Transactional(readOnly = true)
	public void loadExistingPlaceIds() {
		log.info("기존 모든 장소 ID를 메모리 캐시에 로드 중...");
		long startTime = System.currentTimeMillis();
		existingPlaceIds = Collections.synchronizedSet(new HashSet<>(placeRepository.findAllPlaceIds()));
		long endTime = System.currentTimeMillis();
		log.info("{}개의 기존 장소 ID 로드 완료. 소요 시간: {} ms.", existingPlaceIds.size(), (endTime - startTime));
	}

	public boolean contains(String placeId) {
		return existingPlaceIds.contains(placeId);
	}

	public void add(String placeId) {
		existingPlaceIds.add(placeId);
	}
}
