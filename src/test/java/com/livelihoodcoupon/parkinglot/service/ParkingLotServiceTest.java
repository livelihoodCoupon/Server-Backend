package com.livelihoodcoupon.parkinglot.service;

import com.livelihoodcoupon.common.exception.BusinessException;
import com.livelihoodcoupon.parkinglot.dto.NearbySearchRequest;
import com.livelihoodcoupon.parkinglot.dto.ParkingLotDetailResponse;
import com.livelihoodcoupon.parkinglot.dto.ParkingLotNearbyResponse;
import com.livelihoodcoupon.parkinglot.dto.ParkingLotWithDistance;
import com.livelihoodcoupon.parkinglot.entity.ParkingLot;
import com.livelihoodcoupon.parkinglot.repository.ParkingLotRepository;
import com.livelihoodcoupon.search.dto.PageResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class ParkingLotServiceTest {

    @InjectMocks
    private ParkingLotService parkingLotService;

    @Mock
    private ParkingLotRepository parkingLotRepository;

    @Test
    @DisplayName("주변 주차장 검색 서비스 성공")
    void findNearby_success() {
        // given
        NearbySearchRequest request = new NearbySearchRequest();
        request.setLat(37.5665);
        request.setLng(126.9780);
        request.setRadius(1000);
        request.setPage(1);
        request.setSize(10);

        ParkingLotWithDistance projection = mock(ParkingLotWithDistance.class);
        given(projection.getId()).willReturn(1L);
        given(projection.getParkingLotNm()).willReturn("서울시청 주차장");
        given(projection.getDistance()).willReturn(100.0);

        Page<ParkingLotWithDistance> page = new PageImpl<>(Collections.singletonList(projection));
        given(parkingLotRepository.findNearbyParkingLots(anyDouble(), anyDouble(), anyInt(), any(Pageable.class)))
                .willReturn(page);

        // when
        PageResponse<ParkingLotNearbyResponse> response = parkingLotService.findNearby(request);

        // then
        assertThat(response.getContent().get(0).getParkingLotName()).isEqualTo("서울시청 주차장");
        assertThat(response.getContent().get(0).getDistance()).isEqualTo(100.0);
    }

    @Test
    @DisplayName("주차장 상세 정보 조회 서비스 성공")
    void getParkingLotDetails_success() {
        // given
        Long parkingLotId = 1L;
        GeometryFactory geometryFactory = new GeometryFactory();
        Point point = geometryFactory.createPoint(new Coordinate(126.9780, 37.5665));

        ParkingLot parkingLot = ParkingLot.builder()
                .id(parkingLotId)
                .parkingLotNm("테스트 주차장")
                .location(point)
                .build();

        given(parkingLotRepository.findByParkingLotId(parkingLotId)).willReturn(Optional.of(parkingLot));

        // when
        ParkingLotDetailResponse response = parkingLotService.getParkingLotDetails(parkingLotId);

        // then
        assertThat(response.getParkingLotName()).isEqualTo("테스트 주차장");
        assertThat(response.getLat()).isEqualTo(37.5665);
    }

    @Test
    @DisplayName("주차장 상세 정보 조회 시 ID가 없으면 실패")
    void getParkingLotDetails_fail_when_not_found() {
        // given
        Long parkingLotId = 999L;
        given(parkingLotRepository.findByParkingLotId(parkingLotId)).willReturn(Optional.empty());

        // when & then
        assertThrows(BusinessException.class, () -> {
            parkingLotService.getParkingLotDetails(parkingLotId);
        });
    }
}
