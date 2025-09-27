package com.livelihoodcoupon.search.controller;

import java.io.IOException;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.livelihoodcoupon.common.response.CustomApiResponse;
import com.livelihoodcoupon.search.dto.PageResponse;
import com.livelihoodcoupon.search.dto.PlaceSearchResponseDto;
import com.livelihoodcoupon.search.dto.SearchRequestDto;
import com.livelihoodcoupon.search.dto.SearchResponseDto;
import com.livelihoodcoupon.search.service.ElasticService;
import com.livelihoodcoupon.search.service.SearchService;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/api")
public class SearchController {

	private final SearchService search;
	private final ElasticService elasticService;

	/**
	 * redis 이용한 호출
	 * 검색버튼 클릭시 위도, 경도 받아오고 api를 호출해야한다.
	 * **/
	@GetMapping("/search")
	public ResponseEntity<CustomApiResponse<PageResponse<SearchResponseDto>>> search(
		@Valid @ModelAttribute SearchRequestDto request) {

		log.info("RestController search 호출 시작");

		//request 기본 세팅
		request.initDefaults();

		int maxRecordSize = 100;
		int pageSize = 10;
		Page<SearchResponseDto> pageList = search.search(request, pageSize, maxRecordSize);
		PageResponse<SearchResponseDto> searchResponse = new PageResponse<>(pageList, pageSize);

		return ResponseEntity.ok().body(CustomApiResponse.success(searchResponse));

	}

	@GetMapping("/search222")
	public ResponseEntity search222(
		@Valid @ModelAttribute SearchRequestDto request) {

/*		//request 기본 세팅
		request.initDefaults();

		int maxRecordSize = 100;
		int pageSize = 10;
		Page<SearchResponseDTO> pageList = search.search(request, pageSize, maxRecordSize);
		PageResponse<SearchResponseDTO> searchResponse = new PageResponse<>(pageList, pageSize);

		return ResponseEntity.ok().body(CustomApiResponse.success(searchResponse));*/

		return ResponseEntity.ok().build();
	}

	/**
	 * redis 이용한 호출
	 * 검색버튼 클릭시 위도, 경도 받아오고 api를 호출해야한다.
	 * **/
	@GetMapping("/search2")
	public ResponseEntity<CustomApiResponse<PageResponse<PlaceSearchResponseDto>>> searchElastic(
		@Valid @ModelAttribute SearchRequestDto request) throws IOException {

		//request 기본 세팅
		request.initDefaults();

		int maxRecordSize = 100;
		int pageSize = 10;
		Page<PlaceSearchResponseDto> pageList = elasticService.elasticSearch(request, pageSize, maxRecordSize);
		PageResponse<PlaceSearchResponseDto> searchResponse = new PageResponse<>(pageList, pageSize);

		return ResponseEntity.ok().body(CustomApiResponse.success(searchResponse));
	}
}

