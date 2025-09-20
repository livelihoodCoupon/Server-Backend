package com.livelihoodcoupon.search.dto;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.livelihoodcoupon.collector.entity.PlaceEntity;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 목록의 검색결과는 담는 dto
 **/
@Getter
@AllArgsConstructor
public class SearchResponse {

	//@Schema(description = "장소번호", example = "324234")
	private String placeId; // Kakao's unique place ID

	//@Schema(description = "장소명", example = "박준헤어")
	private String placeName;

	//@Schema(description = "도로명주소", example = "서울시 종로구 종로대로 21")
	private String roadAddress;

	//@Schema(description = "지번주소", example = "서울시 종로구 종로동 43")
	private String lotAddress;

	//@Schema(description = "위도", example = "37.609831445")
	private Double lat;

	//@Schema(description = "경도", example = "126.974898")
	private Double lng;

	//@Schema(description = "연락처", example = "070-22-3322")
	private String phone;

	//@Schema(description = "그룹명", example = "주유소,충전소")
	private String categoryGroupName;

	//@Schema(description = "홈페이지주소", example = "http://www.naver.com")
	private String placeUrl;

	public static SearchResponse fromEntity(PlaceEntity placeEntity) {

		return new SearchResponse(
			placeEntity.getPlaceId(),
			placeEntity.getPlaceName(),
			placeEntity.getRoadAddress(),
			placeEntity.getLotAddress(),
			placeEntity.getLat(),
			placeEntity.getLng(),
			placeEntity.getPhone(),
			placeEntity.getCategoryGroupName(),
			placeEntity.getPlaceId()
		);
	}
}
