package com.livelihoodcoupon.batch.listener;

import org.springframework.batch.core.SkipListener;

import com.livelihoodcoupon.batch.PlaceCsvDto;
import com.livelihoodcoupon.search.entity.PlaceDocument;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CustomSkipListener implements SkipListener<PlaceCsvDto, PlaceDocument> {

	@Override
	public void onSkipInRead(Throwable t) {
		log.warn("Reading CSV 중 스킵 발생. Error: {}", t.getMessage());
	}

	@Override
	public void onSkipInProcess(PlaceCsvDto item, Throwable t) {
		log.warn("Processing CSV 중 스킵 발생. Item: {}, Error: {}", item.toString(), t.getMessage());
	}

	@Override
	public void onSkipInWrite(PlaceDocument item, Throwable t) {
		log.warn("Writing to ES 중 스킵 발생. Item ID: {}, Error: {}", item.getPlaceId(), t.getMessage(), t);
	}
}
