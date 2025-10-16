package com.livelihoodcoupon.search.dto;

import com.livelihoodcoupon.search.entity.ParkingLotDocument;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ParkingLotSearchResponseDto {

    private Long id;
    private String parkingLotNm;
    private String roadAddress;
    private String lotAddress;
    private String parkingChargeInfo;
    private Double lat;
    private Double lng;
    private Double distance;

    @Builder
    public ParkingLotSearchResponseDto(Long id, String parkingLotNm, String roadAddress, String lotAddress, String parkingChargeInfo, Double lat, Double lng, Double distance) {
        this.id = id;
        this.parkingLotNm = parkingLotNm;
        this.roadAddress = roadAddress;
        this.lotAddress = lotAddress;
        this.parkingChargeInfo = parkingChargeInfo;
        this.lat = lat;
        this.lng = lng;
        this.distance = distance;
    }

    public static ParkingLotSearchResponseDto fromDocument(ParkingLotDocument doc, double distance) {
        return ParkingLotSearchResponseDto.builder()
                .id(doc.getId())
                .parkingLotNm(doc.getParkingLotNm())
                .roadAddress(doc.getRoadAddress())
                .lotAddress(doc.getLotAddress())
                .parkingChargeInfo(doc.getParkingChargeInfo())
                .lat(doc.getLocation() != null ? doc.getLocation().getLat() : null)
                .lng(doc.getLocation() != null ? doc.getLocation().getLng() : null)
                .distance(distance)
                .build();
    }
}
