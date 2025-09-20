package com.livelihoodcoupon.search.controller;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.livelihoodcoupon.common.response.CustomApiResponse;
import com.livelihoodcoupon.search.dto.PageResponse;
import com.livelihoodcoupon.search.dto.SearchRequest;
import com.livelihoodcoupon.search.dto.SearchResponse;
import com.livelihoodcoupon.search.service.SearchService1;

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
	private final SearchService1 kakaoSearch2;

	/**
	 * 검색버튼 클릭시 위도, 경도 받아오고 api를 호출해야한다.
	 * **/
	@GetMapping("/search")
	public ResponseEntity<CustomApiResponse<PageResponse<SearchResponse>>> search(SearchRequest request){

		int maxRecordSize = 100;
		int pageSize = 10;
		
		//문자열 검색
		Page<SearchResponse> pageList = kakaoSearch2.search(request, pageSize, maxRecordSize);
		
		//pageResponse 만들기
		PageResponse<SearchResponse> searchReponse = new PageResponse<>(pageList, pageSize);
		
		//entity 출력
		return ResponseEntity.ok().body(CustomApiResponse.success(searchReponse));
	}
}
