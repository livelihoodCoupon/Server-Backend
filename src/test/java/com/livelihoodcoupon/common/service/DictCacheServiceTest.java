package com.livelihoodcoupon.common.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

public class DictCacheServiceTest {

	@Test
	void containsAddress_shouldReturnTrueForExistingWord() {
		DictCacheService dictCacheService = new DictCacheService();
		// 내부 Set 초기화 (직접 주입)
		ReflectionTestUtils.setField(dictCacheService, "addressDict", Set.of("서울시"));

		assertTrue(dictCacheService.containsAddress("서울시"));
		assertFalse(dictCacheService.containsAddress("부산시"));
	}

	@Test
	void containsCategory_shouldReturnTrueForExistingWord() {
		DictCacheService dictCacheService = new DictCacheService();
		ReflectionTestUtils.setField(dictCacheService, "categoryDict", Set.of("카페"));

		assertTrue(dictCacheService.containsCategory("카페"));
		assertFalse(dictCacheService.containsCategory("식당"));
	}

/*

	@Test
	void containsAddress_shouldReturnTrueIfWordExists() throws Exception {
		// given: spy로 loadDictFromFile를 mock 처리
		Set<String> mockAddressDict = new HashSet<>();
		mockAddressDict.add("서울시");
		doReturn(mockAddressDict).when(dictCacheService).loadDictFromFile("dict/address_dict.txt");
		doReturn(new HashSet<>()).when(dictCacheService).loadDictFromFile("dict/category_dict.txt");

		// when
		dictCacheService.init(); // @PostConstruct 역할 수행

		// then
		assertThat(dictCacheService.containsAddress("서울시")).isTrue();
		assertThat(dictCacheService.containsAddress("부산시")).isFalse();
	}


	@Test
	void containsCategory_shouldReturnTrueIfWordExists() throws Exception {
		// given
		Set<String> mockCategoryDict = new HashSet<>();
		mockCategoryDict.add("카페");
		doReturn(new HashSet<>()).when(dictCacheService).loadDictFromFile("dict/address_dict.txt");
		doReturn(mockCategoryDict).when(dictCacheService).loadDictFromFile("dict/category_dict.txt");

		// when
		dictCacheService.init();

		// then
		assertThat(dictCacheService.containsCategory("카페")).isTrue();
		assertThat(dictCacheService.containsCategory("음식점")).isFalse();
	}*/

}
