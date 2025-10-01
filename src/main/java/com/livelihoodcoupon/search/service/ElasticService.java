package com.livelihoodcoupon.search.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.livelihoodcoupon.common.dto.Coordinate;
import com.livelihoodcoupon.common.exception.BusinessException;
import com.livelihoodcoupon.common.exception.ErrorCode;
import com.livelihoodcoupon.common.service.KakaoApiService;
import com.livelihoodcoupon.search.dto.AutocompleteDto;
import com.livelihoodcoupon.search.dto.AutocompleteResponseDto;
import com.livelihoodcoupon.search.dto.PlaceSearchResponseDto;
import com.livelihoodcoupon.search.dto.SearchRequestDto;
import com.livelihoodcoupon.search.dto.SearchToken;
import com.livelihoodcoupon.search.entity.PlaceDocument;

import co.elastic.clients.elasticsearch.core.SearchResponse;
import kr.co.shineware.nlp.komoran.constant.DEFAULT_MODEL;
import kr.co.shineware.nlp.komoran.core.Komoran;
import kr.co.shineware.nlp.komoran.model.KomoranResult;
import kr.co.shineware.nlp.komoran.model.Token;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class ElasticService {

	private static String searchNewAddress;
	private final ElasticPlaceService elasticPlaceService;
	private final SearchService searchService;
	private final KakaoApiService kakaoApiService;
	private final AnalyzerTest analyzerTest;
	private final Komoran komoran = new Komoran(DEFAULT_MODEL.FULL);

	public ElasticService(ElasticPlaceService elasticPlaceService, SearchService searchService,
		KakaoApiService kakaoApiService, AnalyzerTest analyzerTest) {
		this.elasticPlaceService = elasticPlaceService;
		this.searchService = searchService;
		this.kakaoApiService = kakaoApiService;
		this.analyzerTest = analyzerTest;
	}

	/**
	 * 단어 자동완성 서비스
	 * @param dto
	 * @param maxRecordSize
	 * @return
	 * @throws IOException
	 */
	public List<AutocompleteResponseDto> elasticSearchAutocomplete(AutocompleteDto dto, int maxRecordSize) throws
		IOException {
		return elasticPlaceService.autocompletePlaceNames(dto, maxRecordSize);
	}

	/**
	 * 엘라스틱 서치 상세내용
	 */
	public PlaceSearchResponseDto elasticSearchDetail(String id, SearchRequestDto dto) throws
		IOException {
		PlaceDocument doc = elasticPlaceService.getPlace(String.valueOf(id));
		if (doc == null || doc.getLocation() == null) {
			throw new BusinessException(ErrorCode.NOT_FOUND, "id=" + id + " 문서를 찾을 수 없습니다.");
		}
		//거리계산
		double distance = searchService.calculateDistance(dto.getLat(), dto.getLng(),
			doc.getLocation().getLat(), doc.getLocation().getLon());
		return PlaceSearchResponseDto.fromEntity(doc, distance);
	}

	/**
	 * 엘라스틱 서치
	 * @param dto
	 * @param pageSize
	 * @param maxRecordSize
	 * @return
	 * @throws IOException
	 */
	public Page<PlaceSearchResponseDto> elasticSearch(SearchRequestDto dto, int pageSize, int maxRecordSize) throws
		IOException {
		String query = dto.getQuery();
		//코모란 자연어 형태소 분리
		List<SearchToken> resultList = analysisChat(query);

		//검색어에 주소가 있을 경우 새로운 위치 가져오기
		log.info("엘라스틱 서치 현재 위치 latitude:{}, longitude:{}", dto.getLat(), dto.getLng());
		if (searchNewAddress != null && !searchNewAddress.isEmpty()) {
			SearchRequestDto result = handleAddressPosition(searchNewAddress, dto).block().getBody();
			log.info("엘라스틱 서치 재수정된 검색위치 latitude:{}, longitude:{}", result.getLat(), result.getLng());
		}

		// 거리 계산을 위한 기준점 설정 (userLat/userLng가 있으면 사용, 없으면 lat/lng 사용)
		double refLat = (dto.getUserLat() != null) ? dto.getUserLat() : dto.getLat();
		double refLng = (dto.getUserLng() != null) ? dto.getUserLng() : dto.getLng();

		//검색하기
		Pageable pageable = PageRequest.of(dto.getPage() - 1, pageSize);
		SearchResponse<PlaceDocument> response = elasticPlaceService.searchPlace(resultList, dto, pageable);

		//dto 변환, 거리계산
		List<PlaceSearchResponseDto> dtoPage = response.hits().hits()
			.stream()
			.limit(maxRecordSize)
			.map(hit -> hit.source())
			.map(place -> {
				// 각 Place에 대해 거리 계산
				return toSearchPosition(place, refLat, refLng);
			}).collect(Collectors.toList());

		long totalHits = response.hits().total() != null ? response.hits().total().value() : 0; //null체크
		long resultTotalHits = Math.min(totalHits, maxRecordSize); //레코드 200개로 자르기
		log.info("엘라스틱 서치 결과 return 총 갯수 : {}", totalHits);
		return new PageImpl<>(dtoPage, pageable, resultTotalHits);
	}

	/**
	 * 검색한 위치해서 상가 위치까지 거리계산
	 * @param doc
	 * @param refLat
	 * @param refLng
	 * @return
	 */
	public PlaceSearchResponseDto toSearchPosition(PlaceDocument doc, double refLat, double refLng) {
		double distance = searchService.calculateDistance(refLat, refLng,
			doc.getLocation().getLat(), doc.getLocation().getLon());
		return PlaceSearchResponseDto.fromEntity(doc, distance);
	}

	/**
	 * 주소로 위도, 경도 재탐색
	 * @param searchNewAddress
	 * @param request
	 * @return
	 */
	public Mono<ResponseEntity<SearchRequestDto>> handleAddressPosition(String searchNewAddress,
		SearchRequestDto request) {
		return kakaoApiService.getCoordinatesFromAddress(searchNewAddress)
			.defaultIfEmpty(Coordinate.builder().lng(0).lat(0).build())
			.flatMap(coordinate -> {
				// request에 좌표 세팅
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

	/**
	 * 단어 형태로 분리
	 * 예) 서울시 강남구 카페 맛집
	 * BeginIndex : 시작위치
	 * EndIndex : 끝위치 :
	 * mar ph : 검색단어
	 * Pos : 단어구분
	 * - 서울시 - NNP	고유 명사 (지명)
	 * - 강남구 - NNP	고유 명사 (지명)
	 * - 카페 - NNG	일반 명사 (장소/시설)
	 * - 맛집 - NNG	일반 명사 (장소/음식점)
	 **/
	public List<SearchToken> analysisChat(String keyword) throws IOException {
		log.info("analysisChat 호출 시작222");

		//단어 형태 자동 분리
		KomoranResult analyzeResultList = komoran.analyze(keyword);

		//analyzeResultList try-can처리
		List<Token> tokenList;
		try {
			// 분리된 문자열 token list 생성
			tokenList = analyzeResultList.getTokenList();
		} catch (NullPointerException e) {
			log.error("형태소 분석 결과 추출 중 오류 발생 - keyword: {}", keyword);
			// 문제를 알리고 빈 리스트로 대체
			return Collections.emptyList();
		}

		List<SearchToken> list = new ArrayList<>();
		StringBuilder builder = new StringBuilder();
		//분리된 문자열 token list 생성
		for (Token token : tokenList) {
			//token 공백여부 체크
			if (token == null) {
				log.warn("token 형태소 정보가 비어있습니다: {}", token);
				continue;
			}

			// 형태소 문자열 체크
			if (token.getMorph() == null || token.getMorph().isBlank()) {
				log.warn("getMorph 형태소 정보가 비어있습니다");
				continue;
			}

			//txt dict 파일에서 필드값 가져오기
			String getFieldName = isAddress(token.getMorph());

			//필드가 address 이면 검색할 주소 build
			if (getFieldName != null && getFieldName.equals("address")) {
				builder.append(" ").append(token.getMorph());
			}

			//Token을 SearchToken으로 통합하기
			SearchToken searchToken = new SearchToken(getFieldName, token);

			//SearchToken 리스트에 추가
			list.add(searchToken);

			log.info("====> 형태소 분리 {}, {} {}, {}, {}", token.getBeginIndex(), token.getEndIndex(), token.getMorph(),
				token.getPos(), getFieldName);
		}
		searchNewAddress = builder.toString();
		return list;
	}

	/**
	 * 단어가 주소여부 체크
	 * @param morph
	 * @return
	 */
	public String isAddress(String morph) throws IOException {
		//txt 파일 메모리 에서 address, category 구분
		return analyzerTest.isCategoryAddress(morph);
	}

}
