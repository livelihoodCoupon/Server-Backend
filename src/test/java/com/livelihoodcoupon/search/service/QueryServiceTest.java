package com.livelihoodcoupon.search.service;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;

import java.util.List;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import com.livelihoodcoupon.place.entity.Place;
import com.livelihoodcoupon.search.dto.SearchRequestDto;
import com.livelihoodcoupon.search.dto.SearchToken;

import kr.co.shineware.nlp.komoran.model.Token;

@ExtendWith(MockitoExtension.class)
public class QueryServiceTest {

	@Mock
	private Root<Place> root;

	@Mock
	private CriteriaQuery<?> query;

	@Mock
	private CriteriaBuilder cb;

	@Mock
	private Order order;

	@Mock
	private RedisService redisService;

	@InjectMocks
	private QueryService queryService;  // 자동으로 redisService Mock 주입됨

	@BeforeEach
	void setUp() {
	}

	@Test
	@DisplayName("Predicate 방식의 쿼리 만들기")
	void buildDynamicSpec_shouldCreateSpecification() {
		// given
		Token token = new Token("한식", "NNP", 0, 3);
		SearchToken searchToken = new SearchToken("category", token);
		List<SearchToken> resultList = List.of(searchToken);

		SearchRequestDto dto = new SearchRequestDto();
		dto.setLat(37.5665);
		dto.setLng(126.9780);
		dto.setRadius(1.0);

		//Order order = mock(Order.class);
		Expression<Object> objectExpr = mock(Expression.class);

		when(cb.like(any(), anyString())).thenReturn(mock(Predicate.class));
		when(cb.isTrue(any())).thenReturn(mock(Predicate.class));
		when(cb.and(any(Predicate[].class))).thenReturn(mock(Predicate.class));
		when(root.get(anyString())).thenReturn(mock(Path.class));
		when(cb.function(eq("ST_MakePoint"), eq(Object.class), any(Expression.class), any(Expression.class)))
			.thenReturn(mock(Expression.class));
		when(cb.function(eq("ST_SetSRID"), eq(Object.class), any(Expression.class), any(Expression.class)))
			.thenReturn(mock(Expression.class));
		when(cb.function(eq("ST_DWithin"), eq(Boolean.class), any(Expression.class), any(Expression.class),
			any(Expression.class)))
			.thenReturn(mock(Predicate.class));
		when(cb.function(eq("ST_Distance"), eq(Double.class), any(Expression.class), any(Expression.class)))
			.thenReturn(mock(Expression.class));
		when(cb.asc(any(Expression.class))).thenReturn(order);
		when(cb.literal(any())).thenReturn(objectExpr);

		// when
		Specification<Place> spec = queryService.buildDynamicSpec(resultList, dto);

		// then
		Predicate result = spec.toPredicate(root, query, cb);
		verify(query).orderBy(any(Order.class));
		assert result != null;
	}
}
