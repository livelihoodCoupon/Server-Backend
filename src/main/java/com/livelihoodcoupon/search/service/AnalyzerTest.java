package com.livelihoodcoupon.search.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.livelihoodcoupon.common.service.DictCacheService;
import com.livelihoodcoupon.search.dto.NoriToken;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.AnalyzeRequest;
import co.elastic.clients.elasticsearch.indices.AnalyzeResponse;
import co.elastic.clients.elasticsearch.indices.analyze.AnalyzeToken;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AnalyzerTest {
	private final ElasticsearchClient client;
	private final DictCacheService dictCacheService;

	public AnalyzerTest(ElasticsearchClient client, DictCacheService dictCacheService) {
		this.client = client;
		this.dictCacheService = dictCacheService;
	}

	public List<NoriToken> analyzeText(String text) throws IOException {
		AnalyzeRequest request = AnalyzeRequest.of(a -> a
			.analyzer("nori")
			.text(text)
		);
		AnalyzeResponse response = client.indices().analyze(request);
		String category;
		List<NoriToken> list = new ArrayList<>();
		for (AnalyzeToken token : response.tokens()) {
			if (token == null)
				continue;
			//단어 분류 체크
			category = isCategoryAddress(token.token());

			list.add(new NoriToken(token.token(), category));
			log.info("====>analyzeText token : {}, category : {}", token.token(), category);
		}
		return list;
	}

	/**
	 * 분리된 단어 address, category 체크
	 * @param token
	 * @return
	 * @throws IOException
	 */
	public String isCategoryAddress(String token) throws IOException {
		String checkCategory;

		if (dictCacheService.containsAddress(token)) {
			log.info("====>isCategoryAddress + → load_address 주소");
			checkCategory = "address";
		} else if (dictCacheService.containsCategory(token)) {
			log.info("====>isCategoryAddress  → category 카테고리");
			checkCategory = "category";
		} else {
			log.info("====>isCategoryAddress + → place_name 상호명 또는 기타");
			checkCategory = "place_name";
		}
		return checkCategory;
	}

	/**
	 * noriken의 주소 가져오기
	 * @param noriTokenList
	 * @return
	 */
	public String getAddress(List<NoriToken> noriTokenList) {
		StringBuilder builder = new StringBuilder();
		for (NoriToken token : noriTokenList) {
			if (Objects.equals(token.getFieldName(), "load_address")) {
				builder.append(token.getToken()).append(" ");
			}
		}
		log.info("====>getAddress 주소찾기 {}", builder.toString());
		return builder.toString();
	}

}
