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
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.livelihoodcoupon.common.dto.Coordinate;
import com.livelihoodcoupon.common.service.KakaoApiService;
import com.livelihoodcoupon.place.entity.Place;
import com.livelihoodcoupon.search.dto.SearchRequestDto;
import com.livelihoodcoupon.search.dto.SearchResponseDto;
import com.livelihoodcoupon.search.dto.SearchToken;
import com.livelihoodcoupon.search.repository.SearchRepository;

import kr.co.shineware.nlp.komoran.constant.DEFAULT_MODEL;
import kr.co.shineware.nlp.komoran.core.Komoran;
import kr.co.shineware.nlp.komoran.model.KomoranResult;
import kr.co.shineware.nlp.komoran.model.Token;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

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

	public Page<SearchResponseDto> search(SearchRequestDto request, int pageSize, int maxRecordSize) {

		String query = request.getQuery();

		//자연어 형태소 검색어 없으면 호출안함.
		List<SearchToken> resultList = analysisChat(query); //자연어 형태소 분리

		//검색어에 주소가 있을 경우 새로운 위치 가져오기
		log.info("현재 위치 latitude:{}, longitude:{}", request.getLat(), request.getLng());
		if (searchNewAddress != null && !searchNewAddress.isEmpty()) {
			SearchRequestDto result = handleAddressPosition(searchNewAddress, request).block().getBody();
			log.info("재수정된 검색위치 latitude:{}, longitude:{}", result.getLat(), result.getLng());
		}
		//검색 쿼리 만들기
		Specification<Place> specList = queryService.buildDynamicSpec(resultList, request);

		Pageable pageable = PageRequest.of(request.getPage() - 1, pageSize, Sort.unsorted());
		Page<Place> results = searchRepository.findAll(specList, pageable);

		// 거리 계산을 위한 기준점 설정 (userLat/userLng가 있으면 사용, 없으면 lat/lng 사용)
		double refLat = (request.getUserLat() != null) ? request.getUserLat() : request.getLat();
		double refLng = (request.getUserLng() != null) ? request.getUserLng() : request.getLng();

		List<SearchResponseDto> dtoPage = results.stream().map(place -> {
			// 각 Place에 대해 거리 계산
			double distance = calculateDistance(refLat, refLng, place.getLocation().getY(), place.getLocation().getX());
			return SearchResponseDto.fromEntity(place, distance);
		}).collect(Collectors.toList());

		log.info("결과 return 총 갯수 : {}", results.getTotalElements());
		return new PageImpl<>(dtoPage, pageable, results.getTotalElements());

	}

	/**
	 * 두 지점 간의 거리를 계산합니다 (Haversine 공식).
	 * PostGIS의 ST_Distance 함수와 유사한 결과를 제공합니다.
	 * @param lat1 첫 번째 지점의 위도
	 * @param lon1 첫 번째 지점의 경도
	 * @param lat2 두 번째 지점의 위도
	 * @param lon2 두 번째 지점의 경도
	 * @return 두 지점 간의 거리 (미터 단위)
	 */
	public double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
		final int R = 6371000; // 지구 반지름 (미터)
		double latDistance = Math.toRadians(lat2 - lat1);
		double lonDistance = Math.toRadians(lon2 - lon1);
		double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
			+ Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
			* Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		return R * c; // 미터 단위
	}

	/**
	 * 주소로 위도, 경도 재탐색
	 * **/
	public Mono<ResponseEntity<SearchRequestDto>> handleAddressPosition(String searchNewAddress,
		SearchRequestDto request) {

		return kakaoApiService.getCoordinatesFromAddress(searchNewAddress)
			.defaultIfEmpty(Coordinate.builder().lng(0).lat(0).build())
			.flatMap(coordinate -> {
				// request에 좌표 세팅
				request.setLat(coordinate.getLat());
				request.setLng(coordinate.getLng());
				log.info("Mono 검색어 기준 좌표 위도: {}, 경도: {}", coordinate.getLat(), coordinate.getLng());
				request.setLat(coordinate.latitude);
				request.setLng(coordinate.longitude);
				log.info("엘라스틱 서치 Mono 검색어 : {},  기준 좌표 위도: {}, 경도: {}", searchNewAddress,
					coordinate.latitude, coordinate.longitude);

				return Mono.just(request);
			})
			.map(ResponseEntity::ok)
			.onErrorResume(e -> {
				log.error("Mono 좌표 검색 중 오류 발생", e);
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
	public List<SearchToken> analysisChat(String keyword) {

		//단어 형태 자동 분리
		KomoranResult analyzeResultList = komoran.analyze(keyword);

		//analyzeResultList try-can처리
		List<Token> tokenList;
		try {
			// 분리된 문자열 token list 생성
			tokenList = analyzeResultList.getTokenList();
		} catch (NullPointerException e) {
			log.error("형태소 분석 결과 추출 중 오류 발생 - keyword: {}", keyword, e);
			// 문제를 알리고 빈 리스트로 대체
			return Collections.emptyList();
		}
		List<SearchToken> list = new ArrayList<>();
		StringBuilder builder = new StringBuilder();

		//분리된 문자열 token list 생성
		for (Token token : tokenList) {
			//token 공백여부 체크
			if (token == null) {
				log.error("token 형태소 정보가 비어있습니다: {}", token);
				continue;
			}

			// 형태소 문자열 체크
			if (token.getMorph() == null || token.getMorph().isBlank()) {
				log.error("getMorph 형태소 정보가 비어있습니다");
				continue;
			}

			//redis 필드값 가져오기
			String redisFieldName = isAddress(token.getMorph(), token.getPos());
			if (redisFieldName != null && redisFieldName.equals("address")) {
				builder.append(" ").append(token.getMorph());
			}

			//Token을 SearchToken으로 통합하기
			SearchToken searchToken = new SearchToken(redisFieldName, token);

			list.add(searchToken);

			log.info("형태소 분리 결과 {}, {} {}, {} ", token.getBeginIndex(), token.getEndIndex(), token.getMorph(),
				token.getPos());
		}
		searchNewAddress = builder.toString();
		return list;
	}

	public String isAddress(String morph, String pos) {
		//redis에서 address, category 구분
		return redisService.getWordInfo(morph);
	}

}
