package com.livelihoodcoupon.common.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing // JPA Auditing을 활성화하는 역할만 담당
@Profile("!test")
@ConditionalOnProperty(name = "spring.jpa.auditing.enabled", havingValue = "true", matchIfMissing = true)
public class JpaAuditingConfig {
}