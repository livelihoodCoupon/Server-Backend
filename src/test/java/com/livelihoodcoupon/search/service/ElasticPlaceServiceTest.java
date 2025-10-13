package com.livelihoodcoupon.search.service;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import com.livelihoodcoupon.search.dto.AnalyzedAddress;
import com.livelihoodcoupon.search.dto.AutocompleteDto;
import com.livelihoodcoupon.search.dto.AutocompleteResponseDto;
import com.livelihoodcoupon.search.dto.SearchRequestDto;
import com.livelihoodcoupon.search.dto.SearchToken;
import com.livelihoodcoupon.search.entity.PlaceDocument;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ShardStatistics;
import co.elastic.clients.elasticsearch.core.GetRequest;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.elasticsearch.core.search.TotalHits;
import co.elastic.clients.util.ObjectBuilder;
import kr.co.shineware.nlp.komoran.model.Token;

@DisplayName("ElasticPlaceServiceTest 단위 테스트")
@ExtendWith(MockitoExtension.class)
public class ElasticPlaceServiceTest {

	@Mock
	Hit<PlaceDocument> hit;
	@Mock
	GetResponse<PlaceDocument> getResponse;

	@Mock
	SearchResponse<PlaceDocument> mockResponse;

	@Mock
	private ElasticsearchClient client;
	@Mock
	private IndexResponse indexResponse; // Elasticsearch index response mock
	@InjectMocks
	private ElasticPlaceService service;

	@BeforeEach
	void setUp() {
	}

	@Test
	@DisplayName("문서 저장 테스트 성공")
	void savePlace_shouldCallIndex() throws IOException {
		// given
		String id = "1";
		PlaceDocument doc = new PlaceDocument();
		doc.setPlaceName("테스트카페");

		doReturn(indexResponse)
			.when(client)
			.index(ArgumentMatchers.<Function<IndexRequest.Builder<PlaceDocument>,
				ObjectBuilder<IndexRequest<PlaceDocument>>>>any());
		//when
		service.savePlace(id, doc);

		//then
		verify(client, times(1)).index(
			ArgumentMatchers.<Function<IndexRequest.Builder<PlaceDocument>, ObjectBuilder<IndexRequest<PlaceDocument>>>>any()
		);
	}

	@Test
	@DisplayName("문서 저장 테스트 실패")
	void getPlace_whenFound_shouldReturnDocument() throws IOException {
		// given
		PlaceDocument doc = new PlaceDocument();
		doc.setPlaceName("테스트카페");
		GetRequest request = new GetRequest.Builder()
			.index("places")
			.id("123")
			.build();

		when(getResponse.found()).thenReturn(true);
		when(getResponse.source()).thenReturn(doc);
		when(client.get(
			ArgumentMatchers.<Function<GetRequest.Builder, ObjectBuilder<GetRequest>>>any(),
			eq(PlaceDocument.class)
		)).thenReturn(getResponse);

		// when
		PlaceDocument result = service.getPlace("1");

		// then
		assertThat(result).isNotNull();
		assertThat(result.getPlaceName()).isEqualTo("테스트카페");
	}

	@Test
	@DisplayName("카테고리 키워드 조회 테스트 성공")
	void searchByCategory_shouldReturnHits() throws IOException {
		// given
		PlaceDocument doc1 = PlaceDocument.builder().id(1).placeName("카페1").build();

		// Hit 객체 생성 (index, id, source 필수)
		Hit<PlaceDocument> hit1 = new Hit.Builder<PlaceDocument>()
			.index("places").id("1").source(doc1).build();

		// HitsMetadata 생성
		HitsMetadata<PlaceDocument> hitsMetadata = new HitsMetadata.Builder<PlaceDocument>()
			.hits(List.of(hit1)).total((TotalHits)null).build();

		// SearchResponse 생성
		SearchResponse<PlaceDocument> mockResponse = new SearchResponse.Builder<PlaceDocument>()
			.took(5L)  // 필수
			.timedOut(false)             // 필수
			.shards(new ShardStatistics.Builder()
				.total(1)
				.successful(1)
				.skipped(0)
				.failed(0)
				.build())
			.hits(hitsMetadata)
			.build();

		when(client.search(
			ArgumentMatchers.<Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>>any(),
			eq(PlaceDocument.class)
		)).thenReturn(mockResponse);

		// when
		List<PlaceDocument> results = service.searchByCategory("카페");

		// then
		assertThat(results)
			.extracting(obj -> ((PlaceDocument)obj).getPlaceName()) // 캐스팅
			.containsExactlyInAnyOrder("카페1");

	}

	@Test
	@DisplayName("자동 완성 테스트 성공")
	void autocompletePlaceNames() throws IOException {
		// given
		AutocompleteDto dto = new AutocompleteDto("강남");
		PlaceDocument doc = new PlaceDocument();
		doc.setRoadAddressSido("서울");
		Hit<PlaceDocument> hit = new Hit.Builder<PlaceDocument>()
			.index("places")
			.id("1")
			.source(doc)
			.build();
		HitsMetadata<PlaceDocument> metadata = new HitsMetadata.Builder<PlaceDocument>()
			.hits(List.of(hit)).build();

		when(mockResponse.hits()).thenReturn(metadata);
		when(client.search(
			ArgumentMatchers.<Function<SearchRequest.Builder, co.elastic.clients.util.ObjectBuilder<SearchRequest>>>any(),
			eq(PlaceDocument.class)
		)).thenReturn(mockResponse);

		// when
		List<AutocompleteResponseDto> results = service.autocompletePlaceNames(dto, 5);

		// then
		assertThat(results.get(0).getWord()).isEqualTo("서울");
	}

	@Test
	@DisplayName("검색테스트 성공")
	void searchPlace_shouldReturnResults() throws IOException {
		// given
		Token token1 = new Token("카페", "NNP", 0, 2);
		Token token2 = new Token("음식점", "NNG", 6, 8);

		List<SearchToken> tokens = List.of(
			new SearchToken("place_name", token1),
			new SearchToken("category", token2)
		);

		SearchRequestDto dto = new SearchRequestDto();
		dto.setLat(37.5665);
		dto.setLng(126.9780);
		dto.setRadius(500.0);
		PageRequest pageable = PageRequest.of(0, 10);
		when(client.search(
			ArgumentMatchers.<Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>>any(),
			eq(PlaceDocument.class)
		)).thenReturn(mockResponse);

		// when
		AnalyzedAddress analyzedAddress = new AnalyzedAddress("", "", "", tokens);
		dto.setUserLat(37.5665); // Add user coordinates for the new signature
		dto.setUserLng(126.9780);
		SearchResponse<PlaceDocument> response = service.searchPlace(analyzedAddress, dto, pageable, dto.getLat(),
			dto.getLng(), dto.getUserLat(), dto.getUserLng());

		// then
		assertNotNull(response);
		verify(client, times(1)).search(
			ArgumentMatchers.<Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>>any(),
			eq(PlaceDocument.class)
		);

	}

}
