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
import com.livelihoodcoupon.search.service.SearchService;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/api")
public class SearchController {
	private final SearchService search;

	/**
	 * redis 이용한 호출
	 * 검색버튼 클릭시 위도, 경도 받아오고 api를 호출해야한다.
	 * **/
	@GetMapping("/search")
	public ResponseEntity<CustomApiResponse<PageResponse<SearchResponse>>> search(
		@Valid @ModelAttribute SearchRequest request) {

		//request 기본 세팅
		request.initDefaults();

		int maxRecordSize = 100;
		int pageSize = 10;
		Page<SearchResponse> pageList = search.search(request, pageSize, maxRecordSize);
		PageResponse<SearchResponse> searchResponse = new PageResponse<>(pageList, pageSize);

		return ResponseEntity.ok().body(CustomApiResponse.success(searchResponse));
	}

}

