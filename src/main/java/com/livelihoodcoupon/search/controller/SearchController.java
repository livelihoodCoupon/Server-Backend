package com.livelihoodcoupon.search.controller;

import java.io.IOException;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.livelihoodcoupon.common.config.SearchProperties;
import com.livelihoodcoupon.common.response.CustomApiResponse;
import com.livelihoodcoupon.search.dto.AutocompleteDto;
import com.livelihoodcoupon.search.dto.AutocompleteResponseDto;
import com.livelihoodcoupon.search.dto.PageResponse;
import com.livelihoodcoupon.search.dto.PlaceSearchResponseDto;
import com.livelihoodcoupon.search.dto.SearchRequestDto;
import com.livelihoodcoupon.search.dto.SearchResponseDto;
import com.livelihoodcoupon.search.dto.SearchServiceResult;
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
	private final SearchProperties searchProperties;

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

		Page<SearchResponseDto> pageList = search.search(request, searchProperties.getPageSize(), searchProperties.getMaxResults());
		PageResponse<SearchResponseDto> searchResponse = new PageResponse<>(pageList, searchProperties.getPageSize(), request.getLat(), request.getLng());

		return ResponseEntity.ok().body(CustomApiResponse.success(searchResponse));

	}

	/**
	 * 엘라스틱 이용한 목록 호출
	 * 검색버튼 클릭시 위도, 경도 받아오고 api를 호출해야한다.
	 * **/
	@GetMapping("/searches")
	public ResponseEntity<CustomApiResponse<PageResponse<PlaceSearchResponseDto>>> searchElastic(
		@Valid @ModelAttribute SearchRequestDto request) throws IOException {

		//request 기본 세팅
		request.initDefaults();

		SearchServiceResult result = elasticService.elasticSearch(request, searchProperties.getPageSize(), searchProperties.getMaxResults());
		Page<PlaceSearchResponseDto> pageList = result.getPage();
		PageResponse<PlaceSearchResponseDto> searchResponse = new PageResponse<>(pageList, searchProperties.getPageSize(), result.getSearchCenterLat(), result.getSearchCenterLng());

		return ResponseEntity.ok().body(CustomApiResponse.success(searchResponse));
	}

	/**
	 * 엘라스틱 이용한 상세내용 출
	 * 검색버튼 클릭시 위도, 경도 받아오고 api를 호출해야한다.
	 * **/
	@GetMapping("/searches/{id}")
	public ResponseEntity<CustomApiResponse<PlaceSearchResponseDto>> searchElasticDetail(
		@Valid @PathVariable String id, SearchRequestDto request) throws IOException {

		PlaceSearchResponseDto dto = elasticService.elasticSearchDetail(id, request);
		return ResponseEntity.ok().body(CustomApiResponse.success(dto));
	}

	/**
	 * 엘라스틱 자동완성 호출
	 * 단어 목록만 추출한다.
	 * **/
	@GetMapping("/suggestions")
	public ResponseEntity<CustomApiResponse<List<AutocompleteResponseDto>>> searchElasticAutocomplete(
		@Valid @ModelAttribute AutocompleteDto request) throws IOException {

		int maxRecordSize = 10;
		List<AutocompleteResponseDto> list = elasticService.elasticSearchAutocomplete(request, maxRecordSize);

		return ResponseEntity.ok().body(CustomApiResponse.success(list));
	}

}

