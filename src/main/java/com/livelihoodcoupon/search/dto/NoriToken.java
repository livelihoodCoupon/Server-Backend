package com.livelihoodcoupon.search.dto;

import kr.co.shineware.nlp.komoran.model.Token;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SearchNoriToken extends Token {
	private String token;
	private String fieldName;

}
