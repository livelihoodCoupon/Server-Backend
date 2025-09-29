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
	private Set<String> addressDict = new HashSet<>();
	private Set<String> categoryDict = new HashSet<>();

	@PostConstruct
	public void init() {
		log.info("==============> DictCacheService init ");
		addressDict = loadDictFromFile("dict/address_dict.txt");
		categoryDict = loadDictFromFile("dict/category_dict.txt");
	}

	private Set<String> loadDictFromFile(String filename) {
		log.info("==============> DictCacheService loadDictFromFile ");
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
		log.info("==============> DictCacheService containsAddress ");
		return addressDict.contains(word);
	}

	public boolean containsCategory(String word) {
		log.info("==============> DictCacheService containsCategory ");
		return categoryDict.contains(word);
	}
}
