package com.livelihoodcoupon;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.livelihoodcoupon.common.test.BaseIntegrationTest;
import com.livelihoodcoupon.search.service.ElasticService;

import kr.co.shineware.nlp.komoran.core.Komoran;

class LivelihoodCouponApplicationTests extends BaseIntegrationTest {

	@MockitoBean
	private Komoran komoran;

	@MockitoBean
	private ElasticService elasticService;

	@Test
	void contextLoads() {
	}

}