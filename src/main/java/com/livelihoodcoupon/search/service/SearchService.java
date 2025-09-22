package com.livelihoodcoupon.search.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.livelihoodcoupon.collector.entity.PlaceEntity;
import com.livelihoodcoupon.collector.service.KakaoApiService;
import com.livelihoodcoupon.search.dto.SearchRequest;
import com.livelihoodcoupon.search.dto.SearchResponse;
import com.livelihoodcoupon.search.dto.SearchToken;
import com.livelihoodcoupon.search.repository.SearchRepository;

import kr.co.shineware.nlp.komoran.constant.DEFAULT_MODEL;
import kr.co.shineware.nlp.komoran.core.Komoran;
import kr.co.shineware.nlp.komoran.model.KomoranResult;
import kr.co.shineware.nlp.komoran.model.Token;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@AllArgsConstructor
public class SearchService {

	private static String searchNewAddress;
	private final RedisService redisService;
	private final SearchRepository searchRepository;
	private final KakaoApiService kakaoApiService;
	private final QueryService queryService;
	private final Komoran komoran = new Komoran(DEFAULT_MODEL.FULL);

	public Page<SearchResponse> search(SearchRequest request, int pageSize, int maxRecordSize) {

		String query = request.getQuery();

		//자연어 형태소 분리
		List<SearchToken> resultList = analysisChat(query);

		//검색어에 주소가 있을 경우 새로운 위치 가져오기
		log.info("주소검색 {}", searchNewAddress);
		if (!searchNewAddress.isEmpty()) {
			kakaoApiService.getCoordinatesFromAddress(searchNewAddress)
				.subscribe(coordinate -> {
					request.setLat(coordinate.latitude);
					request.setLng(coordinate.longitude);
					log.info("검색어를 기준으로 새주소 위치가져오기 위도:{}, 경도:{}", coordinate.latitude, coordinate.longitude);
				});
		}
		log.info("검색위치 latitude:{}, longitude:{}", request.getLat(), request.getLng());

		//검색 쿼리 만들기
		Specification<PlaceEntity> specList = queryService.buildDynamicSpec(resultList, request);

		Pageable pageable = PageRequest.of(request.getPage() - 1, pageSize, Sort.unsorted());
		Page<PlaceEntity> results = searchRepository.findAll(specList, pageable);

		List<SearchResponse> dtoPage = results.stream().map(SearchResponse::fromEntity)
			.collect(Collectors.toList());

		log.info("결과 return 총 갯수 : {}", results.getTotalElements());
		return new PageImpl<>(dtoPage, pageable, results.getTotalElements());
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
	public List<SearchToken> analysisChat(String keyword) {

		//단어 형태 자동 분리
		KomoranResult analyzeResultList = komoran.analyze(keyword);
		//분리된 문자열 token list 생성
		List<Token> tokenList = analyzeResultList.getTokenList();

		List<SearchToken> list = new ArrayList<>();
		//분리 문자열 출력
		StringBuilder builder = new StringBuilder();
		for (Token token : tokenList) {
			//redis 필드값 가져오기
			String redisFieldName = isAddress(token.getMorph(), token.getPos());
			if (redisFieldName != null && redisFieldName.equals("address")) {
				builder.append(" ").append(token.getMorph());
			}
			//Token을 SearchToken으로 통합하기
			SearchToken searchToken = new SearchToken(token);
			searchToken.setFieldName(redisFieldName);

			list.add(searchToken);
			log.info("형태소 분리 결과 {}, {} {}, {} ", token.getBeginIndex(), token.getEndIndex(), token.getMorph(),
				token.getPos());
		}
		searchNewAddress = builder.toString();
		return list;
	}

	public String isAddress(String morph, String pos) {
		return redisService.getWordInfo(morph);
	}
}
