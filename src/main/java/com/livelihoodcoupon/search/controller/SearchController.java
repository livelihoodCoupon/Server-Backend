package com.livelihoodcoupon.search.controller;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.livelihoodcoupon.common.response.CustomApiResponse;
import com.livelihoodcoupon.search.dto.PageResponse;
import com.livelihoodcoupon.search.dto.SearchRequest;
import com.livelihoodcoupon.search.dto.SearchResponse;
import com.livelihoodcoupon.search.service.RedisWordRegister;
import com.livelihoodcoupon.search.service.SearchService;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/api")
public class SearchController {
	private final SearchService search;
	private final RedisWordRegister redisWordRegister;

	/**
	 * redis 이용한 호출
	 * 검색버튼 클릭시 위도, 경도 받아오고 api를 호출해야한다.
	 * **/
	@GetMapping("/v1/search")
	public ResponseEntity<CustomApiResponse<PageResponse<SearchResponse>>> search(
		@Valid @ModelAttribute SearchRequest request){

		request.initDefaults();
		log.info("위도 : {}, 경도 : {} 기본 세팅", request.getLat(), request.getLng());
		
		int maxRecordSize = 100;
		int pageSize = 10;
		Page<SearchResponse> pageList = search.search(request, pageSize, maxRecordSize);
		PageResponse<SearchResponse> searchResponse = new PageResponse<>(pageList, pageSize);

		log.info("위도 : {}, 경도 : {} 기본 세팅", request.getLat(), request.getLng());
		return ResponseEntity.ok().body(CustomApiResponse.success(searchResponse));
	}

	/**
	 * redis 임시 단어등록
	 * 검색하기 전에 redis에 단어 등록한다.
	 *
	 **/
	@GetMapping("/v1/search/redis")
	public ResponseEntity<CustomApiResponse<String>> redisWordRegister(){

		log.info("redis 임시 검색어 등록 시작");
		redisWordRegister.wordRegister();

		return ResponseEntity.ok(CustomApiResponse.success("redis 검색어 등록 완료"));
	}

}
