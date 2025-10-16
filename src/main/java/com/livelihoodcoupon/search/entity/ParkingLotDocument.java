package com.livelihoodcoupon.search.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.livelihoodcoupon.common.dto.Coordinate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParkingLotDocument {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("parking_lot_no")
    private String parkingLotNo;

    @JsonProperty("parking_lot_nm")
    private String parkingLotNm;

    @JsonProperty("parking_lot_se")
    private String parkingLotSe;

    @JsonProperty("parking_lot_type")
    private String parkingLotType;

    @JsonProperty("road_address")
    private String roadAddress;

    @JsonProperty("lot_address")
    private String lotAddress;

    @JsonProperty("parking_capacity")
    private String parkingCapacity;

    @JsonProperty("feeding_se")
    private String feedingSe;

    @JsonProperty("enforce_se")
    private String enforceSe;

    @JsonProperty("oper_day")
    private String operDay;

    @JsonProperty("week_open_time")
    private String weekOpenTime;

    @JsonProperty("week_close_time")
    private String weekCloseTime;

    @JsonProperty("sat_open_time")
    private String satOpenTime;

    @JsonProperty("sat_close_time")
    private String satCloseTime;

    @JsonProperty("holiday_open_time")
    private String holidayOpenTime;

    @JsonProperty("holiday_close_time")
    private String holidayCloseTime;

    @JsonProperty("parking_charge_info")
    private String parkingChargeInfo;

    @JsonProperty("basic_time")
    private String basicTime;

    @JsonProperty("basic_charge")
    private String basicCharge;

    @JsonProperty("add_unit_time")
    private String addUnitTime;

    @JsonProperty("add_unit_charge")
    private String addUnitCharge;

    @JsonProperty("day_ticket_apply_time")
    private String dayTicketApplyTime;

    @JsonProperty("day_ticket_charge")
    private String dayTicketCharge;

    @JsonProperty("payment_method")
    private String paymentMethod;

    @JsonProperty("special_comment")
    private String specialComment;

    @JsonProperty("institution_name")
    private String institutionName;

    @JsonProperty("phone_number")
    private String phoneNumber;

    @JsonProperty("location")
    private Coordinate location;

    @JsonProperty("disabled_parking_zone_yn")
    private String disabledParkingZoneYn;

    @JsonProperty("reference_date")
    private String referenceDate;

    @JsonProperty("institution_code")
    private String institutionCode;

    @JsonProperty("distance")
    private Float distance;
}
