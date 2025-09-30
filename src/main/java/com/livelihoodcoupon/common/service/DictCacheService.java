package com.livelihoodcoupon.common.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import jakarta.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class DictCacheService {
	private Set<String> addressDict;
	private Set<String> categoryDict;

	// 테스트용 생성자
	public DictCacheService(Set<String> addressDict, Set<String> categoryDict) {
		this.addressDict = addressDict;
		this.categoryDict = categoryDict;
	}

	public DictCacheService() {

	}

	@PostConstruct
	public void init() {
		if (addressDict == null) {
			addressDict = loadDictFromFile("dict/address_dict.txt");
		}
		if (categoryDict == null) {
			categoryDict = loadDictFromFile("dict/category_dict.txt");
		}
	}

	private Set<String> loadDictFromFile(String filename) {
		Set<String> dict = new HashSet<>();
		try (BufferedReader br = new BufferedReader(new InputStreamReader(
			Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(filename))))) {
			String line;
			while ((line = br.readLine()) != null) {
				dict.add(line.trim());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return dict;
	}

	public boolean containsAddress(String word) {
		return addressDict.contains(word);
	}

	public boolean containsCategory(String word) {
		return categoryDict.contains(word);
	}
}
