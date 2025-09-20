package com.livelihoodcoupon.search.service;

import java.util.Map;

import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Service
public class SearchRedisService {

	private final String PREFIX_ADDRESS = "address";
	private final String PREFIX_CATEGORY = "category";

	private final RedisTemplate<String, String> redisTemplate;
	private final HashOperations<String, String, String> hashOps;


	public SearchRedisService(RedisTemplate<String, String> redisTemplate) {
		this.redisTemplate = redisTemplate;
		this.hashOps = redisTemplate.opsForHash();
	}

	/**
	 * ## 카테고리 저장형태
	 * HSET word:음식점 field category subfield 1차 parent ""
	 * HSET word:숙박 field category subfield 1차 parent ""
	 * HSET word:한식           field category subfield 2차 parent 음식점
	 * HSET word:일식           field category subfield 2차 parent 음식점
	 *
	 * 주소, 카테고리 단어 저장
	 * @param word     주소 명 (예: 동성로)
	 * @param field    'address'로 고정
	 * @param subfield 3단계 중 어느 단계인지 (시, 구, 동)
	 * @param parent   상위 주소명 (없으면 빈 문자열)
	 */

	public void saveAddressWord(String word, String field, String subfield, String parent) {
		String key = "word:" + word;
		hashOps.put(key, "field", field);
		hashOps.put(key, "subfield", subfield);
		hashOps.put(key, "parent", parent == null ? "" : parent);
	}
	/**
	 * ## 카테고리 저장형태
	 * HSET word:음식점 field category subfield 1차 parent ""
	 * HSET word:숙박 field category subfield 1차 parent ""
	 * HSET word:한식           field category subfield 2차 parent 음식점
	 * HSET word:일식           field category subfield 2차 parent 음식점
	 *
	 * 주소, 카테고리 단어 저장
	 * @param word     주소 명 (예: 동성로)
	 * @param field    'address'로 고정
	 * @param subfield 3단계 중 어느 단계인지 (시, 구, 동)
	 * @param parent   상위 주소명 (없으면 빈 문자열)
	 */
	public void saveCategoryWord(String word, String field, String subfield, String parent) {
		String key = "word:" + word;
		hashOps.put(key, "field", field);
		hashOps.put(key, "subfield", subfield);
		hashOps.put(key, "parent", parent == null ? "" : parent);
	}


	/**
	 * 주소 단어 조회
	 * @param word 주소 단어 (예: 동성로)
	 * @return 해당 단어의 필드 값들 (field, subfield, parent)
	 */
	public Map<String, String> getWordInfo(String word) {
		String key = "word:" + word;
		return hashOps.entries(key);  // 해당 단어의 모든 필드 값 반환
	}
	/**
	 * 주소 단어 필드 조회
	 * @param word 주소 단어 (예: 동성로)
	 * @return address
	 */
	public String getWordInfoField(String word) {
		String key = "word:" + word;
		return hashOps.entries(key).get("field");  // 해당 단어의 모든 필드 값 반환
	}


}
