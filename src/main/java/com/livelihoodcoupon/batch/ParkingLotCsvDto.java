package com.livelihoodcoupon.batch;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ParkingLotCsvDto {
	private String parkingLotNo;
	private String parkingLotNm;
	private String parkingLotSe;
	private String parkingLotType;
	private String roadAddress;
	private String lotAddress;
	private String parkingCapacity;
	private String feedingSe;
	private String enforceSe;
	private String operDay;
	private String weekOpenTime;
	private String weekCloseTime;
	private String satOpenTime;
	private String satCloseTime;
	private String holidayOpenTime;
	private String holidayCloseTime;
	private String parkingChargeInfo;
	private String basicTime;
	private String basicCharge;
	private String addUnitTime;
	private String addUnitCharge;
	private String dayTicketApplyTime;
	private String dayTicketCharge;
	private String monthTicketCharge;
	private String paymentMethod;
	private String specialComment;
	private String institutionName;
	private String phoneNumber;
	private String lat;
	private String lng;
	private String disabledParkingZoneYn;
	private String referenceDate;
	private String institutionCode;
	private String institutionNameKor;

}
