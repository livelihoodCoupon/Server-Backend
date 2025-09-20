package com.livelihoodcoupon.search.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;

import com.livelihoodcoupon.collector.entity.PlaceEntity;

import kr.co.shineware.nlp.komoran.model.Token;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class QueryService {

	private final RedisService redisService;

	/**
	 * 검색어 쿼리 만들기
	 **/
	public Specification<PlaceEntity> buildDynamicSpec(List<Token> resultList, double lat, double lng) {

		return (root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();


			//자연어검색 조건 추가 - 주소, 카테고리, 상가명
			for (Token token : resultList) {
				String word = token.getMorph();
				Map<String, String> wordInfo = redisService.getWordMap(word);
				if (wordInfo == null) continue;

				String field = wordInfo.get("field");
				if (field == null) continue;

				switch (field) {
					case "address":
						predicates.add(cb.like(root.get("roadAddress"), "%" + word + "%"));
						break;
					case "category":
						predicates.add(cb.equal(root.get("categoryGroupName"), word));
						break;
					case "name":
						predicates.add(cb.like(root.get("placeName"), "%" + word + "%"));
						break;
				}
			}

			// 반경 1km
			double radiusKm = 1.0;

			//위도, 경도로 거리계산
			Expression<Double> distanceExpression = cb.function("acos", Double.class,
				cb.sum(
					cb.prod(
						cb.function("cos", Double.class, cb.function("radians", Double.class, cb.literal(lat))),
						cb.prod(
							cb.function("cos", Double.class, cb.function("radians", Double.class, root.get("latitude"))),
							cb.function("cos", Double.class,
								cb.diff(
									cb.function("radians", Double.class, root.get("longitude")),
									cb.function("radians", Double.class, cb.literal(lng))
								)
							)
						)
					),
					cb.prod(
						cb.function("sin", Double.class, cb.function("radians", Double.class, cb.literal(lat))),
						cb.function("sin", Double.class, cb.function("radians", Double.class, root.get("latitude")))
					)
				)
			);

			//위도,경도로 주변 반경 1km 검색 조건 추가
			Expression<Double> distanceKm = cb.prod(cb.literal(Double.valueOf(6371)), distanceExpression);
			predicates.add(cb.lessThan(distanceKm, radiusKm));

/*
			// 거리 계산 (Haversine 공식)
			Expression<Double> latDiff = cb.toRadians(cb.diff(root.get("latitude"), userLat));
			Expression<Double> lngDiff = cb.toRadians(cb.diff(root.get("longitude"), userLng));

			Expression<Double> a = cb.sum(
				cb.prod(cb.sin(cb.quot(latDiff, 2)), cb.sin(cb.quot(latDiff, 2))),
				cb.prod(
					cb.prod(cb.cos(cb.toRadians(cb.literal(userLat))), cb.cos(cb.toRadians(root.get("latitude")))),
					cb.prod(cb.sin(cb.quot(lngDiff, 2)), cb.sin(cb.quot(lngDiff, 2)))
				)
			);

			Expression<Double> c = cb.prod(cb.literal(2.0), cb.atan2(cb.sqrt(a), cb.sqrt(cb.diff(1.0, a))));
			Expression<Double> distanceKm = cb.prod(cb.literal(6371.0), c); // 지구 반지름 6371km
*/

			// 위도, 경도 조건 거래내 검색 추가
			predicates.add(cb.lessThanOrEqualTo(distanceKm, 1.0));

			// 정렬 추가 (가까운 거리순)
			query.orderBy(cb.asc(distanceKm));


			//결과값 리턴
			return cb.and(predicates.toArray(new Predicate[0]));

		};
	}


}
