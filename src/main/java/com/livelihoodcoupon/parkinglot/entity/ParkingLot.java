package com.livelihoodcoupon.parkinglot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.locationtech.jts.geom.Point;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "parking_lot")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParkingLot {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String parkingLotNo;       //주차장 관리번호
	private String parkingLotNm;       //주차장명
	private String parkingLotSe;       //주차장 구분
	private String parkingLotType;     // 주차장 유형
	private String roadAddress;        //소재지 도로명 주소
	private String lotAddress;         //소재지 지번 주소
	private String parkingCapacity;    //주차공간 수
	private String feedingSe;          //급지구분, 1~4급에 따라 상권 파악 가능(1:요금이 비싸고 주변 복잡함 ~ 4: 교외지역이라 한산함)
	private String enforceSe;          //부제시행구분, 자동차 번호에 따라 주차 가능 요일 제도 수행 여부
	private String operDay;            //운영요일
	private String weekOpenTime;       //평일운영시작시각
	private String weekCloseTime;      //평일운영종료시각
	private String satOpenTime;        //토요일운영시작시각
	private String satCloseTime;       //토요일운영종료시각
	private String holidayOpenTime;    //공휴일운영시작시각
	private String holidayCloseTime;   //공휴일운영종료시각
	private String parkingChargeInfo;  //요금정보
	private String basicTime;          //주차기본시간
	private String basicCharge;        //주차기본요금
	private String addUnitTime;        //추가단위시간
	private String addUnitCharge;      //추가단위요금
	private String dayTicketApplyTime; //1일주차권요금적용시간
	private String dayTicketCharge;    //1일주차권요금
	private String paymentMethod;      //결제방법
	private String specialComment;     //특기사항
	private String institutionName;    //관리기관명
	private String phoneNumber;        //전화번호

	// Point 타입 필드 추가
	@Column(columnDefinition = "geography(Point, 4326)")
	private Point location;

	private String disabledParkingZoneYn; //장애인전용주차구역보유여부
	private String referenceDate;         //데이터기준일자
	private String institutionCode;       //제공기관코드
}
