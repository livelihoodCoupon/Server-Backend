package com.livelihoodcoupon.parkinglot.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
public class ParkingLotNearbyResponse {

    private final Long id;
    private final String parkingLotNm;
    private final String roadAddress;
    private final String lotAddress;
    private final String parkingChargeInfo;
    private final Double lat;
    private final Double lng;
    private final Double distance;

    @Builder
    public ParkingLotNearbyResponse(Long id, String parkingLotNm, String roadAddress, String lotAddress, String parkingChargeInfo, Double lat, Double lng, Double distance) {
        this.id = id;
        this.parkingLotNm = parkingLotNm;
        this.roadAddress = roadAddress;
        this.lotAddress = lotAddress;
        this.parkingChargeInfo = parkingChargeInfo;
        this.lat = lat;
        this.lng = lng;
        this.distance = distance;
    }

    public static ParkingLotNearbyResponse from(ParkingLotWithDistance projection) {
        return new ParkingLotNearbyResponse(
                projection.getId(),
                projection.getParkingLotNm(),
                projection.getRoadAddress(),
                projection.getLotAddress(),
                projection.getParkingChargeInfo(),
                projection.getLat(),
                projection.getLng(),
                projection.getDistance()
        );
    }
}
