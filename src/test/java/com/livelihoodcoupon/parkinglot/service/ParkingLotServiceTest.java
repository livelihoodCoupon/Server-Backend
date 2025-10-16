package com.livelihoodcoupon.parkinglot.service;

import com.livelihoodcoupon.common.dto.Coordinate;
import com.livelihoodcoupon.common.exception.BusinessException;
import com.livelihoodcoupon.common.service.KakaoApiService;
import com.livelihoodcoupon.parkinglot.dto.ParkingLotDetailResponse;
import com.livelihoodcoupon.parkinglot.dto.ParkingLotNearbyResponse;
import com.livelihoodcoupon.parkinglot.dto.ParkingLotWithDistance;
import com.livelihoodcoupon.parkinglot.repository.ParkingLotRepository;
import com.livelihoodcoupon.search.dto.PageResponse;
import com.livelihoodcoupon.search.dto.SearchRequestDto;
import com.livelihoodcoupon.search.entity.ParkingLotDocument;
import com.livelihoodcoupon.search.service.ElasticParkingLotService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ParkingLotServiceTest {

    @InjectMocks
    private ParkingLotService parkingLotService;

    @Mock
    private ParkingLotRepository parkingLotRepository;

    @Mock
    private KakaoApiService kakaoApiService;

    @Mock
    private ElasticParkingLotService elasticParkingLotService;

    @Test
    @DisplayName("좌표 기반 주차장 검색 성공 (DB 기반)")
    void searchByQueryOrCoord_withCoords_success() {
        // given
        SearchRequestDto request = SearchRequestDto.builder()
                .lat(37.5665)
                .lng(126.9780)
                .radius(1.0)
                .build();

        ParkingLotWithDistance projection = mock(ParkingLotWithDistance.class);
        Page<ParkingLotWithDistance> page = new PageImpl<>(Collections.singletonList(projection));
        given(parkingLotRepository.findNearbyParkingLots(anyDouble(), anyDouble(), anyDouble(), any(Pageable.class)))
                .willReturn(page);

        // when
        PageResponse<ParkingLotNearbyResponse> response = parkingLotService.searchByQueryOrCoord(request);

        // then
        assertThat(response).isNotNull();
        verify(kakaoApiService, never()).getCoordinatesFromAddress(anyString());
    }

    @Test
    @DisplayName("주차장 상세 정보 조회 성공 (Elasticsearch 기반)")
    void getParkingLotDetails_fromElasticsearch_success() throws IOException {
        // given
        Long parkingLotId = 1L;
        ParkingLotDocument mockDoc = ParkingLotDocument.builder()
                .id(parkingLotId)
                .parkingLotNm("ES 테스트 주차장")
                .roadAddress("ES 테스트 주소")
                .location(new Coordinate(127.0, 37.5))
                .build();

        given(elasticParkingLotService.getParkingLotById(parkingLotId.toString())).willReturn(mockDoc);

        // when
        ParkingLotDetailResponse response = parkingLotService.getParkingLotDetails(parkingLotId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getParkingLotNm()).isEqualTo("ES 테스트 주차장");
        assertThat(response.getRoadAddress()).isEqualTo("ES 테스트 주소");
        assertThat(response.getLat()).isEqualTo(37.5);
        assertThat(response.getLng()).isEqualTo(127.0);
    }

    @Test
    @DisplayName("주차장 상세 정보 조회 실패 - 문서를 찾을 수 없음 (Elasticsearch 기반)")
    void getParkingLotDetails_fromElasticsearch_fail_when_not_found() throws IOException {
        // given
        Long parkingLotId = 999L;
        given(elasticParkingLotService.getParkingLotById(parkingLotId.toString())).willReturn(null);

        // when & then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            parkingLotService.getParkingLotDetails(parkingLotId);
        });
        assertThat(exception.getMessage()).contains("해당 주차장을 찾을 수 없습니다");
    }

    @Test
    @DisplayName("주차장 상세 정보 조회 실패 - Elasticsearch IO 예외 발생")
    void getParkingLotDetails_fromElasticsearch_fail_on_ioexception() throws IOException {
        // given
        Long parkingLotId = 1L;
        given(elasticParkingLotService.getParkingLotById(parkingLotId.toString())).willThrow(new IOException("ES connection failed"));

        // when & then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            parkingLotService.getParkingLotDetails(parkingLotId);
        });
        assertThat(exception.getMessage()).contains("상세 정보 조회 중 오류가 발생했습니다");
    }
}
