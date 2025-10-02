package com.livelihoodcoupon.search.service;

import static org.mockito.Mockito.*;

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
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import com.livelihoodcoupon.place.entity.Place;
import com.livelihoodcoupon.search.dto.SearchRequestDto;
import com.livelihoodcoupon.search.dto.SearchToken;

@ExtendWith(MockitoExtension.class)
public class QueryServiceTest {

	@Mock
	private Root<Place> root;

	@Mock
	private CriteriaQuery<?> query;

	@Mock
	private CriteriaBuilder cb;

	@Mock
	private RedisService redisService;

	@InjectMocks
	private QueryService queryService;  // 자동으로 redisService Mock 주입됨

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		//queryService = new QueryService();

		// --- cb.literal() 모킹 ---
		@SuppressWarnings("unchecked")
		Expression<Double> doubleExpr = mock(Expression.class);
		@SuppressWarnings("unchecked")
		Expression<Object> objectExpr = mock(Expression.class);

		// --- cb.function() 모킹 ---
		@SuppressWarnings("unchecked")
		Expression<Object> funcExpr = mock(Expression.class);
		@SuppressWarnings("unchecked")
		Expression<Double> funcDoubleExpr = mock(Expression.class);
		@SuppressWarnings("unchecked")
		Expression<Boolean> funcBoolExpr = mock(Expression.class);
		@SuppressWarnings("unchecked")
		Order order = mock(Order.class);

		//when(cb.function(anyString(), eq(Object.class), any())).thenReturn(funcExpr);
		//when(cb.function(anyString(), eq(Double.class), any())).thenReturn(funcDoubleExpr);
		//when(cb.function(anyString(), eq(Boolean.class), any())).thenReturn(funcBoolExpr);
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
	}

	@Test
	@DisplayName("Predicate 방식의 쿼리 만들기")
	void buildDynamicSpec_shouldCreateSpecification() {
		// given
		SearchRequestDto dto = new SearchRequestDto();
		dto.setLat(37.5665);
		dto.setLng(126.9780);
		dto.setRadius(500.0);

		SearchToken token = new SearchToken();
		token.setMorph("한식");
		token.setFieldName("category");

		List<SearchToken> tokens = List.of(token);

		// when
		var spec = queryService.buildDynamicSpec(tokens, dto);

		// then
		Predicate result = spec.toPredicate(root, query, cb);

		// 검증: orderBy 호출 여부
		verify(query).orderBy(any(Order.class));

		// 검증: Predicate 반환
		assert result != null;
	}
}
