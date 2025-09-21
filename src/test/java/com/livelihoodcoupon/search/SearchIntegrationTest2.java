package com.livelihoodcoupon.search;

import com.livelihoodcoupon.collector.entity.PlaceEntity;
import com.livelihoodcoupon.common.config.JpaAuditingConfig;
import com.livelihoodcoupon.place.controller.PlaceController;
import com.livelihoodcoupon.search.dto.SearchRequest;
import com.livelihoodcoupon.search.dto.SearchResponse;
import com.livelihoodcoupon.search.repository.SearchRepository;
import com.livelihoodcoupon.search.service.SearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@Transactional
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Search 통합테스트")
public class SearchIntegrationTest2 {

	@Autowired
	private MockMvc mockMvc;

	@Mock
	private SearchService searchService;

	@Autowired
	private SearchRepository searchRepository;

	List<PlaceEntity> places = null;
	@BeforeEach
	void setUp() {
	}

	@Test
	@DisplayName("한단어 테스트 성공")
	void testSearch_OneWord_Success() throws Exception {

		//given
		PlaceEntity place3 = PlaceEntity.builder()
			.placeId("650685922").category("음식점 > 일식 > 일식집")
			.placeName("타마고").roadAddress("서울 중구 을지로 지하 88")
			.lotAddress("서울 중구 을지로2가 149-10").lat(37.56610453).lng(126.9862248)
			.phone("070-8805-0922").categoryGroupName("음식점").build();

		PlaceEntity place4 = PlaceEntity.builder()
			.placeId("853125114").category("음식점 > 카페 > 테마카페 > 디저트카페")
			.placeName("짜드라").roadAddress("서울 중구 을지로 지하 88")
			.lotAddress("서울 중구 을지로2가 149-10").lat(37.56614425).lng(126.9868644)
			.phone("010-8715-6111").categoryGroupName("카페").build();
		List<PlaceEntity> placeList = List.of(place3, place4);
		places = searchRepository.saveAllAndFlush(placeList);

		//when
		ResultActions resultActions = mockMvc.perform(
			get("/api/v1/search")
				.param("query","음식점")
		);

		String responseContent = resultActions.andReturn().getResponse().getContentAsString();
		System.out.println("API 응답 내용: " + responseContent);

		//then
		resultActions.andExpect(status().isOk())
			.andDo(print())
			.andExpect(MockMvcResultMatchers.status().isOk())  // 상태 코드가 200 OK이어야 한다
			.andExpect(MockMvcResultMatchers.jsonPath("$.data.content").isArray())  // JSON path로 배열이 맞는지 검증
			.andExpect(MockMvcResultMatchers.jsonPath("$.data.content.length()").value(2))  // 배열의 길이
			.andExpect(MockMvcResultMatchers.jsonPath("$.data.content[0].placeId").value("650685922"))  // 첫 번째 항목의 placeId 확인
			.andExpect(MockMvcResultMatchers.jsonPath("$.data.content[1].placeName").value("짜드라"));  // 첫 번째 항목의 placeName 확인
	}

	@Test
	@DisplayName("두단어 테스트 성공")
	void testSearch_TwoWord_Success() throws Exception {

		//given
		PlaceEntity place3 = PlaceEntity.builder()
			.placeId("22318916").category("음식점 > 일식 > 참치회")
			.placeName("종로참치").roadAddress("서울 종로구 청계천로 97")
			.lotAddress("서울 종로구 관철동 37-2").lat(37.56836267).lng(126.9884894)
			.phone("02-2265-3737").categoryGroupName("음식점").build();

		PlaceEntity place4 = PlaceEntity.builder()
			.placeId("161332969").category("음식점 > 술집 > 호프,요리주점")
			.placeName("청계뷰호프").roadAddress("서울 종로구 청계천로 97")
			.lotAddress("서울 종로구 관철동 37-2").lat(37.56840862).lng(126.9884419)
			.phone("010-8715-6111").categoryGroupName("음식점").build();
		List<PlaceEntity> placeList = List.of(place3, place4);
		places = searchRepository.saveAllAndFlush(placeList);

		//when
		ResultActions resultActions = mockMvc.perform(
			get("/api/v1/search")
				.param("query","서울시 종로구")
		);

		String responseContent = resultActions.andReturn().getResponse().getContentAsString();
		System.out.println("API 응답 내용: " + responseContent);

		//then
		resultActions.andExpect(status().isOk())
			.andDo(print())
			.andExpect(MockMvcResultMatchers.status().isOk())  // 상태 코드가 200 OK이어야 한다
			.andExpect(MockMvcResultMatchers.jsonPath("$.data.content").isArray())  // JSON path로 배열이 맞는지 검증
			.andExpect(MockMvcResultMatchers.jsonPath("$.data.content.length()").value(2))  // 배열의 길이
			.andExpect(MockMvcResultMatchers.jsonPath("$.data.content[0].placeId").value("22318916"))  // 첫 번째 항목의 placeId 확인
			.andExpect(MockMvcResultMatchers.jsonPath("$.data.content[1].placeName").value("청계뷰호프"));  // 첫 번째 항목의 placeName 확인
	}


	@Test
	@DisplayName("세단어 테스트 성공")
	void testSearch_ThreeWord_Success() throws Exception {

		//given
		PlaceEntity place3 = PlaceEntity.builder()
			.placeId("22318916").category("음식점 > 일식 > 참치회")
			.placeName("종로참치").roadAddress("서울 종로구 청계천로 97")
			.lotAddress("서울 종로구 관철동 37-2").lat(37.56836267).lng(126.9884894)
			.phone("02-2265-3737").categoryGroupName("음식점").build();

		List<PlaceEntity> placeList = List.of(place3);
		places = searchRepository.saveAllAndFlush(placeList);

		//when
		ResultActions resultActions = mockMvc.perform(
			get("/api/v1/search")
				.param("query","서울시 종로구 참치")
		);
		String responseContent = resultActions.andReturn().getResponse().getContentAsString();
		System.out.println("API 응답 내용: " + responseContent);

		//then
		resultActions.andExpect(status().isOk())
			.andDo(print())
			.andExpect(MockMvcResultMatchers.status().isOk())  // 상태 코드가 200 OK이어야 한다
			.andExpect(MockMvcResultMatchers.jsonPath("$.data.content").isArray())  // JSON path로 배열이 맞는지 검증
			.andExpect(MockMvcResultMatchers.jsonPath("$.data.content.length()").value(1))  // 배열의 길이
			.andExpect(MockMvcResultMatchers.jsonPath("$.data.content[0].placeId").value("22318916"))  // 첫 번째 항목의 placeId 확인
			.andExpect(MockMvcResultMatchers.jsonPath("$.data.content[0].placeName").value("종로참치"));  // 첫 번째 항목의 placeName 확인
	}

	@Test
	@DisplayName("검색결과 없는 테스트 성공")
	void testSearch_NoData() throws Exception {

		//given
		PlaceEntity place3 = PlaceEntity.builder()
			.placeId("22318916").category("음식점 > 일식 > 참치회")
			.placeName("종로참치").roadAddress("서울 종로구 청계천로 97")
			.lotAddress("서울 종로구 관철동 37-2").lat(37.56836267).lng(126.9884894)
			.phone("02-2265-3737").categoryGroupName("음식점").build();

		List<PlaceEntity> placeList = List.of(place3);
		places = searchRepository.saveAllAndFlush(placeList);

		//when
		ResultActions resultActions = mockMvc.perform(
			get("/api/v1/search")
				.param("query","서울시 종로구 차아치치치")
		);
		String responseContent = resultActions.andReturn().getResponse().getContentAsString();
		System.out.println("API 응답 내용: " + responseContent);

		//then
		resultActions.andExpect(status().isOk())
			.andDo(print())
			.andExpect(MockMvcResultMatchers.status().isOk())  // 상태 코드가 200 OK이어야 한다
			.andExpect(MockMvcResultMatchers.jsonPath("$.data.content").isEmpty());
	}


	@Test
	@DisplayName("400 검색 실패 - query 파라미터 누락")
	void testSearch_queryMissing() throws Exception {
		ResultActions resultActions =
			mockMvc.perform(get("/api/v1/search"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.message").value("query: 검색어는 필수입니다."))
				.andDo(print());
	}


	@Test
	void testRedisWordRegister() throws Exception {

		//when
		ResultActions resultActions = mockMvc.perform(
			get("/api/v1/search/redis")
		);
		//then
		resultActions.andExpect(status().isOk())
			.andExpect(MockMvcResultMatchers.status().isOk())  // 상태 코드가 200 OK이어야 한다
			.andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true));  // 성공 메시지 확인
	}

}
