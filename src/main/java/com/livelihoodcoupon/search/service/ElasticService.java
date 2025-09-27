package com.livelihoodcoupon.search.service;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.livelihoodcoupon.place.entity.Place;
import com.livelihoodcoupon.search.dto.SearchRequestDTO;
import com.livelihoodcoupon.search.dto.SearchResponseDTO;
import com.livelihoodcoupon.search.dto.SearchToken;

import co.elastic.clients.elasticsearch.core.SearchResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ElasticService {

	private static String searchNewAddress;
	private final ElasticPlaceService elasticPlaceService;
	private final SearchService searchService;

	public ElasticService(ElasticPlaceService elasticPlaceService, SearchService searchService) {
		this.elasticPlaceService = elasticPlaceService;
		this.searchService = searchService;
	}

	public Page<SearchResponseDTO> elasticSearch(SearchRequestDTO dto, int pageSize, int maxRecordSize) throws
		IOException {
		String query = dto.getQuery();

		//자연어 형태소 분리
		List<SearchToken> resultList = searchService.analysisChat(query);

		//검색어에 주소가 있을 경우 새로운 위치 가져오기
		log.info("현재 위치 latitude:{}, longitude:{}", dto.getLat(), dto.getLng());
		if (!searchNewAddress.isEmpty()) {
			SearchRequestDTO result
				= searchService.handleAddressPosition(searchNewAddress, dto).block().getBody();
			log.info("재수정된 검색위치 latitude:{}, longitude:{}", result.getLat(), result.getLng());
		}

		//검색 쿼리 만들기
		Pageable pageable = PageRequest.of(dto.getPage() - 1, pageSize);
		SearchResponse<Place> response = elasticPlaceService.searchPlace(resultList, dto, maxRecordSize, pageable);

		// 거리 계산을 위한 기준점 설정 (userLat/userLng가 있으면 사용, 없으면 lat/lng 사용)
		double refLat = (dto.getUserLat() != null) ? dto.getUserLat() : dto.getLat();
		double refLng = (dto.getUserLng() != null) ? dto.getUserLng() : dto.getLng();

		List<SearchResponseDTO> dtoPage = response.hits().hits()
			.stream()
			.map(hit -> hit.source())
			.map(place -> {
				//SearchResponseDTO.fromEntity(place, dto.getRadius());

				// 각 Place에 대해 거리 계산
				double distance = searchService.calculateDistance(refLat, refLng,
					place.getLocation().getY(), place.getLocation().getX());
				return SearchResponseDTO.fromEntity(place, distance);

			}) // 또는 dto -> mapper.toEntity(dto)
			.collect(Collectors.toList());

		long totalHits = response.hits().total() != null ? response.hits().total().value() : 0;
		//int totalPages = (int)Math.ceil((double)totalHits / pageSize);

		return new PageImpl<>(dtoPage, pageable, totalHits);

		////////////////////////////////////
/*		//엘라스틱 서치 추가
		//Page<Place> results = searchRepository.findAll(specList, pageable);

		// 거리 계산을 위한 기준점 설정 (userLat/userLng가 있으면 사용, 없으면 lat/lng 사용)
		double refLat = (request.getUserLat() != null) ? request.getUserLat() : request.getLat();
		double refLng = (request.getUserLng() != null) ? request.getUserLng() : request.getLng();

		List<SearchResponseDTO> dtoPage = results.stream().map(place -> {
			// 각 Place에 대해 거리 계산
			double distance = calculateDistance(refLat, refLng,
				place.getLocation().getY(), place.getLocation().getX());
			return SearchResponseDTO.fromEntity(place, distance);
		}).collect(Collectors.toList());

		log.info("결과 return 총 갯수 : {}", results.getTotalElements());
		return new PageImpl<>(dtoPage, pageable, results.getTotalElements());*/

	}
}
