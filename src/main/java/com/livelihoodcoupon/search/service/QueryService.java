package com.livelihoodcoupon.search.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.livelihoodcoupon.place.entity.Place;
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
	public Specification<Place> buildDynamicSpec(List<SearchToken> resultList,
		SearchRequest request) { // Changed from PlaceEntity

		double lat = request.getLat();
		double lng = request.getLng();
		double radius = request.getRadius();

		return (root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();

			//자연어검색 조건 추가 - 주소, 카테고리, 상가명
			for (SearchToken token : resultList) {
				String word = token.getMorph();
				String field = (token.getFieldName() == null ? "placeName" : token.getFieldName());
				log.info("필드 조건 시작 필드명 : {}, 단어: {}, 위도:{}, 경도:{}", field, word, request.getLat(), request.getLng());

				switch (field) {
					case "address":
						predicates.add(
							cb.like(root.get("roadAddress"), "%" + word.substring(0, word.length() - 1) + "%"));
						break;
					case "category":
						predicates.add(cb.like(root.get("category"), "%" + word + "%"));
						break;
					default:
						predicates.add(cb.like(root.get("placeName"), "%" + word + "%"));
						break;
				}
			}

			// PostGIS Point 객체 생성 (검색 중심점)
			Expression<?> searchPoint = cb.function("ST_SetSRID", Object.class,
				cb.function("ST_MakePoint", Object.class, cb.literal(lng), cb.literal(lat)),
				cb.literal(4326)
			);

			// PostGIS 공간 필터링 (반경 내)
			predicates.add(cb.isTrue(cb.function("ST_DWithin", Boolean.class,
				root.get("location"), // Place의 location 필드 (geography 타입)
				searchPoint,
				cb.literal(radius) // radius는 이미 미터 단위로 가정
			)));

			// 거리 계산을 위한 기준점 (userLat/userLng가 있으면 사용, 없으면 lat/lng 사용)
			Expression<?> distanceRefPoint;
			if (request.getUserLat() != null && request.getUserLng() != null) {
				distanceRefPoint = cb.function("ST_SetSRID", Object.class,
					cb.function("ST_MakePoint", Object.class, cb.literal(request.getUserLng()),
						cb.literal(request.getUserLat())),
					cb.literal(4326)
				);
			} else {
				distanceRefPoint = searchPoint;
			}

			// 거리 계산 (미터 단위)
			Expression<Double> calculatedDistance = cb.function("ST_Distance", Double.class,
				root.get("location"),
				distanceRefPoint
			);

			// 쿼리 결과에 거리 포함 (select 절에 추가)
			// JPA Criteria API에서 select 절에 추가하는 방식은 Specification에서 직접적으로 지원하지 않음.
			// 여기서는 정렬 조건으로만 사용.

			Objects.requireNonNull(query).orderBy(cb.asc(calculatedDistance));
			return cb.and(predicates.toArray(new Predicate[0]));

		};
	}
}

