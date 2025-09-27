package com.livelihoodcoupon.search.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class CategoryDictLoader {

	private final Set<String> categorySet = new HashSet<>();

	public CategoryDictLoader() {
		loadCategoryDict();
	}

	private void loadCategoryDict() {
		try {
			log.info("CategoryDictLoader dict/category_dict.txt start");
			ClassPathResource resource = new ClassPathResource("dict/category_dict.txt");
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
				String line;
				while ((line = reader.readLine()) != null) {
					String[] tokens = line.trim().split("\\s+");
					Collections.addAll(categorySet, tokens);
				}
			}
		} catch (IOException e) {
			throw new RuntimeException("category_dict.txt 파일을 읽는 중 오류 발생", e);
		}
	}

	public boolean contains(String token) {
		log.info("CategoryDictLoader categorySet.contains(token)=" + categorySet.contains(token));
		return categorySet.contains(token);
	}

	// 테스트용 메서드
	public void printAll() {
		System.out.println("=== 주소 사전 ===");
		categorySet.forEach(System.out::println);
	}
}
