package com.livelihoodcoupon.parkinglot.controller;

import com.livelihoodcoupon.common.response.CustomApiResponse;
import com.livelihoodcoupon.parkinglot.dto.NearbySearchRequest;
import com.livelihoodcoupon.parkinglot.dto.ParkingLotNearbyResponse;
import com.livelihoodcoupon.parkinglot.service.ParkingLotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/parking-lots")
@RequiredArgsConstructor
public class ParkingLotController {

    private final ParkingLotService parkingLotService;

    /**
     * 지정된 좌표와 반경 내의 주변 주차장을 검색합니다.
     * @param request 위도(lat), 경도(lng), 반경(radius)을 포함하는 요청 객체
     * @return 거리순으로 정렬된 주차장 목록
     */
    @GetMapping("/nearby")
    public ResponseEntity<CustomApiResponse<List<ParkingLotNearbyResponse>>> getNearbyParkingLots(
            @Valid @ModelAttribute NearbySearchRequest request) {

        List<ParkingLotNearbyResponse> nearbyParkingLots = parkingLotService.findNearby(request);
        return ResponseEntity.ok(CustomApiResponse.success(nearbyParkingLots));
    }
}
