package com.livelihoodcoupon.search.dto;

import kr.co.shineware.nlp.komoran.model.Token;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SearchToken extends Token {
	private String fieldName;

	public SearchToken(String fieldName, Token token) {
		super();
		this.setFieldName(fieldName);
		this.setMorph(token.getMorph());
		this.setBeginIndex(token.getBeginIndex());
		this.setEndIndex(token.getEndIndex());
		this.setPos(token.getPos());
	}

}
