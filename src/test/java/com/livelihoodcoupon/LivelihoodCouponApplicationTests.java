package com.livelihoodcoupon;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.livelihoodcoupon.common.test.BaseIntegrationTest;
import com.livelihoodcoupon.search.service.ElasticService;

import kr.co.shineware.nlp.komoran.core.Komoran;

class LivelihoodCouponApplicationTests extends BaseIntegrationTest {

	//komoran, elasticservice는
	//out of memory 오류로 인해서 메인 test에서 bean으로 등록한다.
	@MockitoBean
	private Komoran komoran;

	@MockitoBean
	private ElasticService elasticService;

	@Test
	void contextLoads() {
	}

}