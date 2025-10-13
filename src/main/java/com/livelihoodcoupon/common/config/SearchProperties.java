package com.livelihoodcoupon.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@ConfigurationProperties(prefix = "search")
@Getter
@Setter
public class SearchProperties {
	private int pageSize;
	private int maxResults;
}
