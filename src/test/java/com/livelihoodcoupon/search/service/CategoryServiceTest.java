package com.livelihoodcoupon.search.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.livelihoodcoupon.search.dto.CategoryDto;
import com.livelihoodcoupon.search.entity.Category;
import com.livelihoodcoupon.search.repository.CategoryRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryService 단위 테스트")
class CategoryServiceTest {
	@Mock
	CategoryRepository categoryRepository;
	@InjectMocks
	CategoryService categoryService;

	@Test
	@DisplayName("카테고리 상위 10개 가져오기 테스트 성공")
	void findTopCategory_success() {
		//given
		int recordCount = 1;
		Category category = Category.builder()
			.id(1L)
			.categoryName("음식점")
			.count(1).build();
		List<Category> list = List.of(category);
		Page<Category> page = new PageImpl<>(list);
		when(categoryRepository.findByCategoryNameIsNotNull(any(Pageable.class)))
			.thenReturn(page);

		//when
		List<CategoryDto> result = categoryService.findTopCategory(recordCount);

		//then
		assertEquals(1, list.size());
		assertEquals("음식점", result.get(0).getCategoryName());
	}

	@Test
	@DisplayName("postgres에 카테고리 업데이트 성공")
	void saveOrIncrement_update() {
		//given
		String categoryName = "음식점";
		Category category = Category.builder()
			.id(1L)
			.categoryName("음식점")
			.count(1).build();
		List<Category> list = List.of(category);
		Page<Category> page = new PageImpl<>(list);
		when(categoryRepository.existsByCategoryNameContaining(categoryName)).thenReturn(true);

		//when
		categoryService.saveOrIncrement(categoryName);

		// then
		verify(categoryRepository, times(1)).incrementCountNative(categoryName); // count 증가
		//verify(categoryRepository, times(1)).save(any(Category.class)); // save도 호출됨
	}

	@Test
	@DisplayName("postgres에 카테고리 저장 성공")
	void saveOrIncrement_save() {
		//given
		String categoryName = "음식점";
		Category category = Category.builder().build();
		List<Category> list = List.of(category);
		Page<Category> page = new PageImpl<>(list);
		when(categoryRepository.existsByCategoryNameContaining(categoryName)).thenReturn(false);

		//when
		categoryService.saveOrIncrement(categoryName);

		// then
		verify(categoryRepository, times(1)).save(any(Category.class)); // save도 호출됨
	}

	@Test
	@DisplayName("카테고리 전체 갯수 조회 성공")
	void isExistCount_success() {
		// given
		when(categoryRepository.count()).thenReturn(5L);

		// when
		long result = categoryService.isExistCount();

		// then
		assertEquals(5L, result); // 결과값 검증
		verify(categoryRepository, times(1)).count(); // count() 호출 여부 검증
	}

	@Test
	@DisplayName("카테고리 전체 지우기 성공")
	void deleteCategory_success() {
		// when
		categoryService.deleteCategory();

		// then
		verify(categoryRepository, times(1)).deleteAll(); // count() 호출 여부 검증
	}
}
