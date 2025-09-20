package com.livelihoodcoupon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

//@EnableJpaAuditing
@SpringBootApplication
public class LivelihoodCouponApplication {

	public static void main(String[] args) {
		SpringApplication.run(LivelihoodCouponApplication.class, args);
	}

}
