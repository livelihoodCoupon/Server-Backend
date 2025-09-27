package com.livelihoodcoupon.common.config;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;

/**
 * 엘라스틱 서치 연결 설정
 * .eng 파일에 등록하고  application.yml 파일에 등록하고 사용한다.
 */
@Configuration
public class ElasticSearchConfig {

	private final ObjectMapper objectMapper;
	@Value("${elasticsearch.host}")
	private String host;
	@Value("${elasticsearch.port}")
	private int port;
	@Value("${elasticsearch.scheme}")
	private String scheme;

	// 생성자 주입 방식으로 ObjectMapper 받기
	public ElasticSearchConfig(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
		// 혹시 여기서 세팅 안되어 있으면 추가 설정 가능 (선택)
		this.objectMapper.registerModule(new JavaTimeModule());
		this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		System.out.println("===========> Registered modules: " + objectMapper.getRegisteredModuleIds());
	}

	@Bean
	public RestClient restClient() {
		System.out.println("=====>restClient===host:" + host + ", port:" + port + ", scheme:" + scheme);
		return RestClient.builder(
			new HttpHost(host, port, scheme)
		).build();
	}

	@Bean
	public ElasticsearchTransport transport(RestClient restClient) {
		return new RestClientTransport(
			restClient,
			new JacksonJsonpMapper(objectMapper)
		);
	}

	@Bean
	public ElasticsearchClient elasticsearchClient(ElasticsearchTransport transport) {
		return new ElasticsearchClient(transport);
	}
}
