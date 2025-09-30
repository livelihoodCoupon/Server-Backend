package com.livelihoodcoupon;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.livelihoodcoupon.common.test.BaseIntegrationTest;
import com.livelihoodcoupon.search.service.ElasticService;

import kr.co.shineware.nlp.komoran.core.Komoran;

@SpringBootTest(classes = {}) // 빈 클래스 없이 context 최소화
class LivelihoodCouponApplicationTests extends BaseIntegrationTest {

	@MockitoBean
	private Komoran komoran;

	@MockitoBean
	private ElasticService elasticService;

	@Test
	void contextLoads() {
	}

}
