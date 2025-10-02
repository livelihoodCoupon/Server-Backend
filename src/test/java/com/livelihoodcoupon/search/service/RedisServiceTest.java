package com.livelihoodcoupon.search.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;

public class RedisServiceTest {

	@Mock
	private RedisTemplate<String, String> redisTemplate;

	@Mock
	private HashOperations<String, String, String> hashOps;

	@InjectMocks
	private RedisService redisService;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		when(redisTemplate.opsForHash()).thenReturn((HashOperations)hashOps);
	}

	@Test
	@DisplayName("주소 단어 저장 테스트")
	void saveWord_shouldCallHashOpsPut() {
		// given
		String word = "동성로";
		String field = "address";
		String subfield = "동";
		String parent = "중구";

		// when
		redisService.saveWord(word, field, subfield, parent);

		// then
		String expectedKey = "word:" + word;
		verify(hashOps).put(expectedKey, "field", field);
		verify(hashOps).put(expectedKey, "subfield", subfield);
		verify(hashOps).put(expectedKey, "parent", parent);
	}

	@Test
	@DisplayName("주소 단어 조회 테스트")
	void getWordMap_shouldReturnHashMap() {
		// given
		String word = "동성로";
		String key = "word:" + word;

		Map<String, String> mockMap = new HashMap<>();
		mockMap.put("field", "address");
		mockMap.put("subfield", "동");
		mockMap.put("parent", "중구");

		when(hashOps.entries(key)).thenReturn(mockMap);

		// when
		Map<String, String> result = redisService.getWordMap(word);

		// then
		assertThat(result).isEqualTo(mockMap);
		verify(hashOps).entries(key);
	}

	@Test
	@DisplayName("주소 단어 필드 조회 테스트 - 값 존재")
	void getWordInfo_shouldReturnField() {
		// given
		String word = "동성로";
		String key = "word:" + word;

		Map<String, String> mockMap = new HashMap<>();
		mockMap.put("field", "address");

		when(hashOps.entries(key)).thenReturn(mockMap);

		// when
		String result = redisService.getWordInfo(word);

		// then
		assertThat(result).isEqualTo("address");
		verify(hashOps, times(1)).entries(key);
	}

	@Test
	@DisplayName("주소 단어 필드 조회 테스트 - 값 없을 때")
	void getWordInfo_shouldReturnEmptyWhenNoField() {
		// given
		String word = "없는주소";
		String key = "word:" + word;

		Map<String, String> mockMap = new HashMap<>(); // 빈 map
		when(hashOps.entries(key)).thenReturn(mockMap);

		// when
		String result = redisService.getWordInfo(word);

		// then
		assertThat(result).isEmpty();
		verify(hashOps, times(1)).entries(key);
	}
}
