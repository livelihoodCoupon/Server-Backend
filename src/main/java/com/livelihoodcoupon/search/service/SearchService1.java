package com.livelihoodcoupon.search.service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.livelihoodcoupon.collector.entity.PlaceEntity;
import com.livelihoodcoupon.search.dto.SearchRequest;
import com.livelihoodcoupon.search.dto.SearchResponse;
import com.livelihoodcoupon.search.repository.SearchRepository;

import kr.co.shineware.nlp.komoran.constant.DEFAULT_MODEL;
import kr.co.shineware.nlp.komoran.core.Komoran;
import kr.co.shineware.nlp.komoran.model.KomoranResult;
import kr.co.shineware.nlp.komoran.model.Token;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class SearchService {

	private final RedisService redisService;
	private final SearchRepository searchRepository;

	public Page<SearchResponse> search(SearchRequest searchRequest, int pageSize, int maxRecordSize){

		//검색어 가져오기
		String keyword = searchRequest.getKeyword();

		//자연어 형태소 분리
 		List<Token> resultList = analysisChat(keyword);

		//검색 쿼리 만들기
		QueryService queryService = new QueryService(redisService);
		Specification<PlaceEntity> specList = queryService.buildDynamicSpec(resultList,
			searchRequest.getLng(), searchRequest.getLng());

		//정렬지정
		Pageable pageable = PageRequest.of(searchRequest.getPage(), pageSize);

		//DB실행
		Page<PlaceEntity> results = searchRepository.findAll(specList, pageable);

		//dto로 변환
		List<SearchResponse> dtoPage = results
			.getContent()
			.stream()
			.limit(maxRecordSize)
			.map(SearchResponse::fromEntity)
			.collect(Collectors.toList());

		//페이지 내보내기
		return new PageImpl<>(dtoPage, pageable, results.getTotalElements());
	}


	/**
	 * 단어 형태로 분리
	 * 서울시 강남구 카페 맛집
	 * 서울시 - NNP	고유 명사 (지명)
	 * 강남구 - NNP	고유 명사 (지명)
	 * 카페 - NNG	일반 명사 (장소/시설)
	 * 맛집 - NNG	일반 명사 (장소/음식점)
	 **/
	public List<Token> analysisChat(String keyword){
		//komoran 
		Komoran komoran = new Komoran(DEFAULT_MODEL.FULL);
		
		//문자 분리
		KomoranResult analyzeResultList = komoran.analyze(keyword);

		//분리된 문자열 token list 생성
		List<Token> tokenList = analyzeResultList.getTokenList();
		
		//분리 문자열 출력
		for (Token token : tokenList) {
			//( 0,  2) 일식/NNG
			//( 3,  6) 음식점/NNG
			System.out.format("(%2d, %2d) %s/%s\n", token.getBeginIndex(), token.getEndIndex(), token.getMorph(), token.getPos());
		}
		return tokenList;
	}

}
