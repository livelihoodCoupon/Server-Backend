package com.livelihoodcoupon.search.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class RedisWordRegister {
	private final RedisService redisService;
	private final CategoryService categoryService;

	public RedisWordRegister(RedisService redisService, CategoryService categoryService) {
		this.redisService = redisService;
		this.categoryService = categoryService;
	}

	// 테스트용: 파일 리소스 생성
	protected ClassPathResource createResource(String path) {
		return new ClassPathResource(path);
	}

	/**
	 * redis에 address, category 등록
	 * @param redisKey
	 * @throws IOException
	 */
	public void fileWordRegister(String redisKey) throws IOException {
		// Redis 키
		String pre = "word:";
		String filePath = "dict/" + redisKey + "_dict.txt";

		// 이미 초기화된 경우는 건너뜀 (옵션)
		if (equals(redisService.getRedisTemplate().hasKey(pre + redisKey))) {
			System.out.println("Redis에 address_dict가 이미 초기화되어 있습니다.");
			return;
		}

		// 파일 읽기
		ClassPathResource resource = createResource(filePath);
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
			String line;
			while ((line = reader.readLine()) != null) {
				// 공백 제거 및 비어있지 않은 단어만
				String trimmed = line.trim();
				if (!trimmed.isEmpty()) {
					redisService.saveWord(trimmed, redisKey, "", "");
				}
			}
		}
		System.out.println("Redis에 address_dict 초기화 완료!");
	}

	/**
	 * postgres db에 category 등록하는 로직
	 * @param redisKey
	 * @throws IOException
	 */
	public void fileWordRegister2(String redisKey) throws IOException {

		//읽을 파일 지정
		String filePath = "dict/" + redisKey + "_dict.txt";

		//기존에 카테고리가 있으면 삭제하고 다시등록한다.
		long count = categoryService.isExistCount();
		if (count > 0) {
			categoryService.deleteCategory();
		}

		// 파일 읽기
		ClassPathResource resource = createResource(filePath);
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
			String line;
			while ((line = reader.readLine()) != null) {
				// 공백 제거 및 비어있지 않은 단어만
				String trimmed = line.trim();

				//category_dict.txt에서 상위 20개까지 가져와서 category 테이블 필드와 비교해서 등록 또는 수정하기
				categoryService.saveOrIncrement(trimmed);
			}
		}
	}

}
