package com.livelihoodcoupon.search.service;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.DeleteIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ElasticIndexService {

	private final ElasticsearchClient elasticsearchClient;

	public void createIndices() {
		log.info("Elasticsearch 인덱스 생성 절차를 시작합니다.");
		createIndexIfNotExists("places", "elasticsearch/places-mapping.json");
		createIndexIfNotExists("places_autocomplete", "elasticsearch/places_autocomplete-mapping.json");
		log.info("Elasticsearch 인덱스 생성 절차를 완료했습니다.");
	}

	public void deleteIndices() throws IOException {
		log.warn("Elasticsearch 인덱스 삭제 절차를 시작합니다.");
		deleteIndexIfExist("places");
		deleteIndexIfExist("places_autocomplete");
		log.warn("Elasticsearch 인덱스 삭제 절차를 완료했습니다.");
	}

	private void createIndexIfNotExists(String indexName, String mappingFilePath) {
		try {
			// 1. 인덱스 존재 여부 확인
			boolean exists = elasticsearchClient.indices().exists(ExistsRequest.of(e -> e.index(indexName))).value();

			if (exists) {
				log.info("인덱스 '{}'는 이미 존재합니다. 생성을 건너뜁니다.", indexName);
				return;
			}

			// 2. 인덱스가 없으면 매핑 파일과 함께 생성
			log.info("인덱스 '{}'를 생성합니다. 매핑 파일: '{}'", indexName, mappingFilePath);
			try (InputStream mapping = new ClassPathResource(mappingFilePath).getInputStream()) {
				CreateIndexRequest request = CreateIndexRequest.of(b -> b
					.index(indexName)
					.withJson(mapping)
				);
				elasticsearchClient.indices().create(request);
				log.info("인덱스 '{}'를 성공적으로 생성했습니다.", indexName);
			} catch (IOException e) {
				log.error("매핑 파일 '{}'을 읽는 중 오류가 발생했습니다.", mappingFilePath, e);
				throw new RuntimeException(e);
			}

		} catch (IOException e) {
			log.error("인덱스 '{}' 생성 중 Elasticsearch 통신 오류가 발생했습니다.", indexName, e);
			throw new RuntimeException(e);
		}
	}

	private void deleteIndexIfExist(String indexName) throws IOException {
		boolean exists = elasticsearchClient.indices().exists(ExistsRequest.of(e -> e.index(indexName))).value();
		if (exists) {
			log.warn("인덱스 '{}'를 삭제합니다.", indexName);
			elasticsearchClient.indices().delete(DeleteIndexRequest.of(d -> d.index(indexName)));
			log.warn("인덱스 '{}'를 성공적으로 삭제했습니다.", indexName);
		} else {
			log.info("인덱스 '{}'가 존재하지 않아 삭제를 건너뜁니다.", indexName);
		}
	}
}
