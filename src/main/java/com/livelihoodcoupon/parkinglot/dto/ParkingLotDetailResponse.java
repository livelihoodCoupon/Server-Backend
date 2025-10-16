package com.livelihoodcoupon.parkinglot.dto;

import com.livelihoodcoupon.parkinglot.entity.ParkingLot;

import com.livelihoodcoupon.search.entity.ParkingLotDocument;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class ParkingLotDetailResponse {

    private final Long id;
    private final String parkingLotNm;       // 주차장명
    private final String roadAddress;        // 도로명 주소
    private final String lotAddress;         // 지번 주소
    private final String parkingCapacity;    // 주차공간 수
    private final String operDay;            // 운영요일
    private final String weekOpenTime;       // 평일 운영 시작 시각
    private final String weekCloseTime;      // 평일 운영 종료 시각
    private final String satOpenTime;        // 토요일 운영 시작 시각
    private final String satCloseTime;       // 토요일 운영 종료 시각
    private final String holidayOpenTime;    // 공휴일 운영 시작 시각
    private final String holidayCloseTime;   // 공휴일 운영 종료 시각
    private final String parkingChargeInfo;         // 요금 정보
    private final String paymentMethod;      // 결제 방법
    private final String specialComment;     // 특기사항
    private final String phoneNumber;        // 전화번호
    private final Double lat;                // 위도
    private final Double lng;                // 경도


    public static ParkingLotDetailResponse fromParkingLot(ParkingLot parkingLot) {
        return ParkingLotDetailResponse.builder()
                .id(parkingLot.getId())
                .parkingLotNm(parkingLot.getParkingLotNm())
                .roadAddress(parkingLot.getRoadAddress())
                .lotAddress(parkingLot.getLotAddress())
                .parkingCapacity(parkingLot.getParkingCapacity())
                .operDay(parkingLot.getOperDay())
                .weekOpenTime(parkingLot.getWeekOpenTime())
                .weekCloseTime(parkingLot.getWeekCloseTime())
                .satOpenTime(parkingLot.getSatOpenTime())
                .satCloseTime(parkingLot.getSatCloseTime())
                .holidayOpenTime(parkingLot.getHolidayOpenTime())
                .holidayCloseTime(parkingLot.getHolidayCloseTime())
                .parkingChargeInfo(parkingLot.getParkingChargeInfo())
                .paymentMethod(parkingLot.getPaymentMethod())
                .specialComment(parkingLot.getSpecialComment())
                .phoneNumber(parkingLot.getPhoneNumber())
                .lat(parkingLot.getLocation() != null ? parkingLot.getLocation().getY() : null)
                .lng(parkingLot.getLocation() != null ? parkingLot.getLocation().getX() : null)
                .build();
    }

    public static ParkingLotDetailResponse fromDocument(ParkingLotDocument doc) {
        return ParkingLotDetailResponse.builder()
                .id(doc.getId())
                .parkingLotNm(doc.getParkingLotNm())
                .roadAddress(doc.getRoadAddress())
                .lotAddress(doc.getLotAddress())
                .parkingCapacity(doc.getParkingCapacity())
                .operDay(doc.getOperDay())
                .weekOpenTime(doc.getWeekOpenTime())
                .weekCloseTime(doc.getWeekCloseTime())
                .satOpenTime(doc.getSatOpenTime())
                .satCloseTime(doc.getSatCloseTime())
                .holidayOpenTime(doc.getHolidayOpenTime())
                .holidayCloseTime(doc.getHolidayCloseTime())
                .parkingChargeInfo(doc.getParkingChargeInfo())
                .paymentMethod(doc.getPaymentMethod())
                .specialComment(doc.getSpecialComment())
                .phoneNumber(doc.getPhoneNumber())
                .lat(doc.getLocation() != null ? doc.getLocation().getLat() : null)
                .lng(doc.getLocation() != null ? doc.getLocation().getLng() : null)
                .build();
    }
}