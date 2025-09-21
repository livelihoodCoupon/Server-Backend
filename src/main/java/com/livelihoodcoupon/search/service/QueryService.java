package com.livelihoodcoupon.search.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.livelihoodcoupon.collector.entity.PlaceEntity;
import com.livelihoodcoupon.search.dto.SearchRequest;
import com.livelihoodcoupon.search.dto.SearchToken;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@AllArgsConstructor
public class QueryService {

	private final RedisService redisService;

	/**
	 * 검색어 쿼리 만들기
	 **/
	public Specification<PlaceEntity> buildDynamicSpec(List<SearchToken> resultList, SearchRequest request) {

		double lat = request.getLat();
		double lng = request.getLng();
		double radius = request.getRadius();

		return (root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();

			//자연어검색 조건 추가 - 주소, 카테고리, 상가명
			for (SearchToken token : resultList) {
				String word = token.getMorph();
				String field = (token.getFieldName()==null? "placeName":token.getFieldName());
				log.info("필드 조건 시작 필드명 : {}, 단어: {}, 위도:{}, 경도:{}",field, word, request.getLat(), request.getLng());

				switch (field) {
					case "address":
						predicates.add(cb.like(root.get("roadAddress"), "%"+word.substring(0,word.length()-1)+"%"));
						break;
					case "category":
						predicates.add(cb.like(root.get("category"), "%"+word+"%"));
						break;
					default:
						predicates.add(cb.like(root.get("placeName"), "%"+word+"%"));
						break;
				}
			}

			//위도, 경도로 거리계산
			Expression<Double> distanceExpression = cb.function("acos", Double.class,
				cb.sum(
					cb.prod(
						cb.function("cos", Double.class, cb.function("radians", Double.class, cb.literal(lat))),
						cb.prod(
							cb.function("cos", Double.class, cb.function("radians", Double.class, root.get("lat"))),
							cb.function("cos", Double.class,
								cb.diff(
									cb.function("radians", Double.class, root.get("lng")),
									cb.function("radians", Double.class, cb.literal(lng))
								)
							)
						)
					),
					cb.prod(
						cb.function("sin", Double.class, cb.function("radians", Double.class, cb.literal(lat))),
						cb.function("sin", Double.class, cb.function("radians", Double.class, root.get("lat")))
					)
				)
			);

			//위도,경도로 주변 반경 1km 검색 조건 추가
			Expression<Double> distanceKm = cb.prod(cb.literal(6371.0), distanceExpression);
			predicates.add(cb.lessThan(distanceKm, radius));

			Objects.requireNonNull(query).orderBy(cb.asc(distanceKm));
			return cb.and(predicates.toArray(new Predicate[0]));

		};
	}


}
