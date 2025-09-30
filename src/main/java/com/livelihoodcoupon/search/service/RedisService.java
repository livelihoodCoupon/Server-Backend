package com.livelihoodcoupon.search.service;

import java.util.Map;

import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@Service
public class RedisService {

	private final String searchPrefix = "word:";

	private final RedisTemplate<String, String> redisTemplate;
	//private final HashOperations<String, String, String> hashOps;

	public RedisService(RedisTemplate<String, String> redisTemplate) {
		this.redisTemplate = redisTemplate;
		//this.hashOps = redisTemplate.opsForHash();
	}

	private HashOperations<String, String, String> hashOps() {
		return redisTemplate.opsForHash();
	}

	/**
	 * ## 주소 저장
	 * word:서울 field address subfield 시 parent ""
	 * word:종로구 field address subfield 구 parent 서울
	 * word:강남구 field address subfield 구 parent 서울
	 * word:대구 field address subfield 시 parent ""
	 * word:중구 field address subfield 구 parent 대구
	 * word:동구 field address subfield 구 parent 대구
	 * word:동성로 field address subfield 동 parent 중구
	 * 주소단어 저장
	 * @param word 주소 명 (예: 동성로)
	 * @param field address or category
	 * @param subfield 3단계 중 어느 단계인지 (시, 구, 동)
	 * @param parent   상위 주소명 (없으면 빈 문자열)
	 */
	public void saveWord(String word, String field, String subfield, String parent) {
		String key = searchPrefix + word;
		hashOps().put(key, "field", field);
		hashOps().put(key, "subfield", subfield);
		hashOps().put(key, "parent", parent == null ? "" : parent);
		//log.info("Redis 저장: key={}, field={}, subfield={}, parent={}", key, field, subfield, parent);
	}

	/**
	 * 주소 단어 조회
	 * @param word 주소 단어 (예: 동성로)
	 * @return 해당 단어의 필드 값들 (field, subfield, parent)
	 */
	public Map<String, String> getWordMap(String word) {
		String key = searchPrefix + word;
		return hashOps().entries(key);  // 해당 단어의 모든 필드 값 반환
	}

	/**
	 * 주소 단어 필드 조회
	 * @param word 주소 단어 (예: 동성로)
	 * @return address
	 */
	public String getWordInfo(String word) {
		String key = searchPrefix + word;
		String value = hashOps().entries(key).get("field");
		if (value == null || value.isEmpty() || value.trim().isEmpty()) {
			return "";
		}
		return value;  // 해당 단어의 모든 필드 값 반환
	}
}
