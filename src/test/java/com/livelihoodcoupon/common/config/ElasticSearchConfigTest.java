package com.livelihoodcoupon.common.config;

import static org.assertj.core.api.Assertions.*;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.transport.ElasticsearchTransport;

public class ElasticSearchConfigTest {

	@Test
	@DisplayName("엘라스틱 접속여부 테스트 성공")
	void elasticSearchBeans_shouldBeCreated() {
		// given
		ObjectMapper objectMapper = new ObjectMapper();
		ElasticSearchConfig config = new ElasticSearchConfig(objectMapper);

		// set @Value fields manually since 단위 테스트에서는 스프링 환경이 없음
		ReflectionTestUtils.setField(config, "host", "localhost");
		ReflectionTestUtils.setField(config, "port", 9200);
		ReflectionTestUtils.setField(config, "scheme", "http");

		// when
		RestClient restClient = config.restClient();
		ElasticsearchTransport transport = config.transport(restClient);
		ElasticsearchClient client = config.elasticsearchClient(transport);

		// then
		assertThat(restClient).isNotNull();
		assertThat(transport).isNotNull();
		assertThat(client).isNotNull();

		// host/port/scheme 값 확인
		HttpHost httpHost = restClient.getNodes().get(0).getHost();
		assertThat(httpHost.getHostName()).isEqualTo("localhost");
		assertThat(httpHost.getPort()).isEqualTo(9200);
		assertThat(httpHost.getSchemeName()).isEqualTo("http");
	}
}
