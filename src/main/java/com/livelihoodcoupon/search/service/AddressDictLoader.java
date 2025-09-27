package com.livelihoodcoupon.search.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AddressDictLoader {

	private final Set<String> addressSet = new HashSet<>();

	public AddressDictLoader() {
		loadAddressDict();
	}

	private void loadAddressDict() {
		try {
			log.info("AddressDictLoader dict/address_dict.txt start");
			ClassPathResource resource = new ClassPathResource("dict/address_dict.txt");
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
				String line;
				while ((line = reader.readLine()) != null) {
					String[] tokens = line.trim().split("\\s+");
					Collections.addAll(addressSet, tokens);
				}
			}
		} catch (IOException e) {
			throw new RuntimeException("address_dict.txt 파일을 읽는 중 오류 발생", e);
		}
	}

	public boolean contains(String token) {
		log.info("AddressDictLoader categorySet.contains(token)=" + addressSet.contains(token));
		return addressSet.contains(token);
	}

	// 테스트용 메서드
	public void printAll() {
		System.out.println("=== 주소 사전 ===");
		addressSet.forEach(System.out::println);
	}
}
