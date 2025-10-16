
package com.livelihoodcoupon.search.service;

import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.livelihoodcoupon.common.dto.Coordinate;
import com.livelihoodcoupon.common.exception.BusinessException;
import com.livelihoodcoupon.common.exception.ErrorCode;
import com.livelihoodcoupon.common.service.KakaoApiService;
import com.livelihoodcoupon.parkinglot.dto.NearbySearchRequest;
import com.livelihoodcoupon.parkinglot.dto.ParkingLotNearbyResponse;
import com.livelihoodcoupon.parkinglot.service.ParkingLotService;
import com.livelihoodcoupon.search.dto.*;
import com.livelihoodcoupon.search.entity.ParkingLotDocument;
import com.livelihoodcoupon.search.entity.PlaceDocument;
import kr.co.shineware.nlp.komoran.constant.DEFAULT_MODEL;
import kr.co.shineware.nlp.komoran.core.Komoran;
import kr.co.shineware.nlp.komoran.model.KomoranResult;
import kr.co.shineware.nlp.komoran.model.Token;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ElasticService {

	private final ElasticPlaceService elasticPlaceService;
	private final ElasticParkingLotService elasticParkingLotService;
	private final SearchService searchService;
	private final KakaoApiService kakaoApiService;
	private final AnalyzerTest analyzerTest;
	private final RedisService redisService;
	private final ParkingLotService parkingLotService;
	private final Komoran komoran = new Komoran(DEFAULT_MODEL.FULL);

	public ElasticService(ElasticPlaceService elasticPlaceService, ElasticParkingLotService elasticParkingLotService, SearchService searchService,
						KakaoApiService kakaoApiService, AnalyzerTest analyzerTest, RedisService redisService,
						ParkingLotService parkingLotService) {
		this.elasticPlaceService = elasticPlaceService;
		this.elasticParkingLotService = elasticParkingLotService;
		this.searchService = searchService;
		this.kakaoApiService = kakaoApiService;
		this.analyzerTest = analyzerTest;
		this.redisService = redisService;
		this.parkingLotService = parkingLotService;
	}

	public SearchServiceResult<ParkingLotSearchResponseDto> elasticSearchParkingLots(SearchRequestDto dto, int pageSize, int maxRecordSize) throws IOException {
		if (Objects.isNull(dto.getLat()) || Objects.isNull(dto.getLng())) {
			throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "좌표(lat, lng)는 필수 입력값입니다.");
		}

		String query = dto.getQuery();
		AnalyzedAddress analyzedAddress = analysisChat(query);

		Pageable pageable = PageRequest.of(dto.getPage() - 1, pageSize);

		if (pageable.getOffset() >= maxRecordSize) {
			Page<ParkingLotSearchResponseDto> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, maxRecordSize);
			return new SearchServiceResult<>(emptyPage, dto.getLat(), dto.getLng());
		}

		double userLat = (dto.getUserLat() != null) ? dto.getUserLat() : dto.getLat();
		double userLng = (dto.getUserLng() != null) ? dto.getUserLng() : dto.getLng();
		double searchLat = dto.getLat();
		double searchLng = dto.getLng();

		SearchResponse<ParkingLotDocument> response = elasticParkingLotService.searchParkingLot(analyzedAddress, dto, pageable,
				searchLat, searchLng, userLat, userLng);

		List<ParkingLotSearchResponseDto> dtoPage = response.hits().hits()
				.stream()
				.map(hit -> {
					ParkingLotDocument doc = hit.source();
					double distance = searchService.calculateDistance(userLat, userLng,
							doc.getLocation().getLat(), doc.getLocation().getLng());
					return ParkingLotSearchResponseDto.fromDocument(doc, distance);
				}).collect(Collectors.toList());

		long totalHits = response.hits().total() != null ? response.hits().total().value() : 0;
		long resultTotalHits = Math.min(totalHits, maxRecordSize);

		Page<ParkingLotSearchResponseDto> page = new PageImpl<>(dtoPage, pageable, resultTotalHits);
		return new SearchServiceResult<>(page, searchLat, searchLng);
	}


	public List<AutocompleteResponseDto> elasticSearchAutocomplete(AutocompleteDto dto, int maxRecordSize) throws
		IOException {
		List<AutocompleteResponseDto> list = elasticPlaceService.autocompletePlaceNames(dto, maxRecordSize);
		if (list == null || list.isEmpty()) {
			throw new BusinessException(ErrorCode.NOT_FOUND, "검색 결과가 없습니다.");
		}
		return list;
	}

	public PlaceSearchResponseDto elasticSearchDetail(String id, SearchRequestDto dto) throws
		IOException {
		PlaceDocument doc = elasticPlaceService.getPlace(String.valueOf(id));
		if (doc == null || doc.getLocation() == null) {
			throw new BusinessException(ErrorCode.NOT_FOUND, "id=" + id + " 문서를 찾을 수 없습니다.");
		}
		double distance = searchService.calculateDistance(dto.getLat(), dto.getLng(),
			doc.getLocation().getLat(), doc.getLocation().getLng());
		System.out.println("444");
		return PlaceSearchResponseDto.fromEntity(doc, distance);
	}

	public SearchServiceResult<PlaceSearchResponseDto> elasticSearch(SearchRequestDto dto, int pageSize, int maxRecordSize) throws
		IOException {
		String query = dto.getQuery();
		AnalyzedAddress analyzedAddress = analysisChat(query);

		Pageable pageable = PageRequest.of(dto.getPage() - 1, pageSize);

		if (pageable.getOffset() >= maxRecordSize) {
			Page<PlaceSearchResponseDto> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, maxRecordSize);
			return new SearchServiceResult<>(emptyPage, dto.getLat(), dto.getLng());
		}

		double userLat = (dto.getUserLat() != null) ? dto.getUserLat() : dto.getLat();
		double userLng = (dto.getUserLng() != null) ? dto.getUserLng() : dto.getLng();

		double searchLat = dto.getLat();
		double searchLng = dto.getLng();
		log.info("엘라스틱 서치 현재 위치 latitude:{}, longitude:{}", searchLat, searchLng);

		if (dto.isForceLocationSearch() || dto.isDisableGeoFilter()) {
			log.info("forceLocationSearch 또는 disableGeoFilter가 true이므로, 검색어 내 지역 정보는 무시하고 dto.lat/lng를 검색 중심으로 사용합니다.");
		} else {
			String fullAddressFromAnalysis = analyzedAddress.getFullAddress();
			if (fullAddressFromAnalysis != null && !fullAddressFromAnalysis.trim().isEmpty()) {
				SearchRequestDto result = handleAddressPosition(fullAddressFromAnalysis, dto).block().getBody();
				searchLat = result.getLat();
				searchLng = result.getLng();
				log.info("엘라스틱 서치 재수정된 검색위치 latitude:{}, longitude:{}", searchLat, searchLng);

				com.livelihoodcoupon.common.dto.Coord2RegionCodeResponse regionInfo = kakaoApiService.getRegionInfo(
					searchLng, searchLat);
				if (regionInfo != null && regionInfo.getDocuments() != null && !regionInfo.getDocuments().isEmpty()) {
					com.livelihoodcoupon.common.dto.Coord2RegionCodeResponse.RegionDocument document = regionInfo.getDocuments()
						.get(0);
					analyzedAddress = new AnalyzedAddress(analyzedAddress.getFullAddress(),
						document.getRegion1DepthName(),
						document.getRegion2DepthName(), analyzedAddress.getResultList());
				}
			}
		}
		SearchResponse<PlaceDocument> response = elasticPlaceService.searchPlace(analyzedAddress, dto, pageable,
			searchLat, searchLng, userLat, userLng);

		List<PlaceSearchResponseDto> dtoPage = response.hits().hits()
			.stream()
			.map(hit -> hit.source())
			.map(place -> {
				return toSearchPosition(place, userLat, userLng);
			}).collect(Collectors.toList());

		long totalHits = response.hits().total() != null ? response.hits().total().value() : 0;
		long resultTotalHits = Math.min(totalHits, maxRecordSize);
		log.info("Total Hits from ES: {}, Result Total Hits after capping: {}", totalHits, resultTotalHits);
		log.info("엘라스틱 서치 결과 return 총 갯수 : {}", totalHits);

		Page<PlaceSearchResponseDto> page = new PageImpl<>(dtoPage, pageable, resultTotalHits);
		return new SearchServiceResult<>(page, searchLat, searchLng);
	}

	public PageResponse<ParkingLotNearbyResponse> searchParkingLotsNearPlace(SearchRequestDto request) throws IOException {
		double centerLat;
		double centerLng;

		if (!org.springframework.util.StringUtils.hasText(request.getQuery()) && request.getLat() != null && request.getLng() != null) {
			centerLat = request.getLat();
			centerLng = request.getLng();
			log.info("좌표 기반으로 주차장 검색을 시작합니다. Lat: {}, Lng: {}", centerLat, centerLng);
		}
		else if (org.springframework.util.StringUtils.hasText(request.getQuery())) {
			log.info("장소 검색어 '{}' 기반으로 주차장 검색을 시작합니다.", request.getQuery());
			SearchServiceResult<PlaceSearchResponseDto> placeSearchResult = this.elasticSearch(request, 1, 1);
			centerLat = placeSearchResult.getSearchCenterLat();
			centerLng = placeSearchResult.getSearchCenterLng();
		}
		else {
			throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "좌표 또는 검색어가 필요합니다.");
		}

		NearbySearchRequest parkingRequest = new NearbySearchRequest();
		parkingRequest.setLat(centerLat);
		parkingRequest.setLng(centerLng);
		parkingRequest.setRadius(1.0); // 1km 반경
		parkingRequest.setPage(request.getPage());
		parkingRequest.setSize(10);

		return parkingLotService.findNearby(parkingRequest);
	}

	public PlaceSearchResponseDto toSearchPosition(PlaceDocument doc, double refLat, double refLng) {
		double distance = searchService.calculateDistance(refLat, refLng,
			doc.getLocation().getLat(), doc.getLocation().getLng());
		return PlaceSearchResponseDto.fromEntity(doc, distance);
	}

	public Mono<ResponseEntity<SearchRequestDto>> handleAddressPosition(String searchNewAddress,
		SearchRequestDto request) {
		return kakaoApiService.getCoordinatesFromAddress(searchNewAddress)
			.defaultIfEmpty(Coordinate.builder().lng(0).lat(0).build())
			.flatMap(coordinate -> {
				request.setLat(coordinate.getLat());
				request.setLng(coordinate.getLng());
				log.info("엘라스틱 서치 Mono 검색어 : {},  기준 좌표 위도: {}, 경도: {}", searchNewAddress, coordinate.getLat(),
					coordinate.getLng());
				return Mono.just(request);
			})
			.map(r -> ResponseEntity.ok(r))
			.onErrorResume(e -> {
				log.error("엘라스틱 서치 Mono 좌표 검색 중 오류 발생", e);
				return Mono.just(ResponseEntity.ok(request));
			});
	}

	public AnalyzedAddress analysisChat(String keyword) throws IOException {
		log.info("analysisChat 호출 시작222");

		KomoranResult analyzeResultList = komoran.analyze(keyword);

		List<Token> tokenList;
		try {
			tokenList = analyzeResultList.getTokenList();
		} catch (NullPointerException e) {
			log.error("형태소 분석 결과 추출 중 오류 발생 - keyword: {}", keyword);
			return new AnalyzedAddress("", "", "", Collections.emptyList());
		}

		List<SearchToken> list = new ArrayList<>();
		StringBuilder builder = new StringBuilder();
		for (Token token : tokenList) {
			if (token == null) {
				log.warn("token 형태소 정보가 비어있습니다: {}", token);
				continue;
			}

			if (token.getMorph() == null || token.getMorph().isBlank()) {
				log.warn("getMorph 형태소 정보가 비어있습니다");
				continue;
			}

			String getFieldName = isAddress(token.getMorph());

			if (getFieldName != null && getFieldName.equals("address")) {
				builder.append(" ").append(token.getMorph());
			}

			SearchToken searchToken = new SearchToken(getFieldName, token);

			list.add(searchToken);

			log.info("====> 형태소 분리 {}, {} {}, {}, {}", token.getBeginIndex(), token.getEndIndex(), token.getMorph(),
				token.getPos(), getFieldName);
		}
		return new AnalyzedAddress(builder.toString().trim(), null, null, list);
	}

	public String isAddress(String morph) throws IOException {
		return redisService.getWordInfo(morph);
	}

}

