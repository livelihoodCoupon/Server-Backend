package com.livelihoodcoupon.parkinglot.dto;

import com.livelihoodcoupon.parkinglot.dto.ParkingLotWithDistance;
import lombok.Builder;
import lombok.Getter;

@Getter
public class ParkingLotNearbyResponse {

    private final Long id;
    private final String parkingLotName;
    private final String roadAddress;
    private final String phoneNumber;
    private final String feeInfo;
    private final Double lat;
    private final Double lng;
    private final Double distance; // 미터 단위 거리

    @Builder
    public ParkingLotNearbyResponse(Long id, String parkingLotName, String roadAddress, String phoneNumber, String feeInfo, Double lat, Double lng, Double distance) {
        this.id = id;
        this.parkingLotName = parkingLotName;
        this.roadAddress = roadAddress;
        this.phoneNumber = phoneNumber;
        this.feeInfo = feeInfo;
        this.lat = lat;
        this.lng = lng;
        this.distance = distance;
    }

    public static ParkingLotNearbyResponse from(ParkingLotWithDistance projection) {
        return ParkingLotNearbyResponse.builder()
                .id(projection.getId())
                .parkingLotName(projection.getParkingLotNm())
                .roadAddress(projection.getRoadAddress())
                .phoneNumber(projection.getPhoneNumber())
                .feeInfo(projection.getParkingChargeInfo())
                .lat(projection.getLocation() != null ? projection.getLocation().getY() : null)
                .lng(projection.getLocation() != null ? projection.getLocation().getX() : null)
                .distance(projection.getDistance())
                .build();
    }
}
