package com.livelihoodcoupon.search.dto;

import kr.co.shineware.nlp.komoran.model.Token;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SearchToken extends Token {
	private String fieldName;

	public SearchToken(Token token) {
		super();
		this.setMorph(token.getMorph());
		this.setBeginIndex(token.getBeginIndex());
		this.setEndIndex(token.getEndIndex());
		this.setPos(token.getPos());
	}

}
