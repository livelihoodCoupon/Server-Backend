package com.livelihoodcoupon.search.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.livelihoodcoupon.common.response.CustomApiResponse;
import com.livelihoodcoupon.search.dto.PageResponse;
import com.livelihoodcoupon.search.dto.SearchRequest;
import com.livelihoodcoupon.search.dto.SearchResponse;
import com.livelihoodcoupon.search.service.SearchService;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@AllArgsConstructor
@RequestMapping("/api/v1/search")
public class SearchController {
/*
	/api/v1/search
	/api/v1/category/list
	/api/v1/category/reg

*/
	private final SearchService kakaoSearch;

	@GetMapping("/search")
	public ResponseEntity<CustomApiResponse<PageResponse<SearchResponse>>> search(SearchRequest searchRequest){

		log.info("검색 시작");
		kakaoSearch.search(searchRequest);
		return null;
	}
}
