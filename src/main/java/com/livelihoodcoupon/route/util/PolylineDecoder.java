package com.livelihoodcoupon.route.util;

import java.util.ArrayList;
import java.util.List;

import com.livelihoodcoupon.common.dto.Coordinate;

/**
 * OSRM Polyline 디코더
 * OSRM API에서 반환하는 polyline 문자열을 실제 좌표 배열로 변환합니다.
 *
 * <p>OSRM은 Google Polyline Algorithm을 사용하여 경로를 압축합니다.
 * 이 클래스는 해당 알고리즘을 역으로 적용하여 좌표를 복원합니다.</p>
 *
 * <h3>사용 예시:</h3>
 * <pre>{@code
 * String polyline = "encoded_polyline_string";
 * List<Coordinate> coordinates = PolylineDecoder.decode(polyline);
 * }</pre>
 */
public class PolylineDecoder {

	private static final double PRECISION = 1e5;

	/**
	 * Polyline 문자열을 좌표 리스트로 디코딩합니다.
	 *
	 * @param polyline 인코딩된 polyline 문자열
	 * @return 디코딩된 좌표 리스트
	 * @throws IllegalArgumentException polyline이 null이거나 잘못된 형식인 경우
	 */
	public static List<Coordinate> decode(String polyline) {
		if (polyline == null || polyline.trim().isEmpty()) {
			return new ArrayList<>();
		}

		List<Coordinate> coordinates = new ArrayList<>();
		int index = 0;
		int lat = 0;
		int lng = 0;

		while (index < polyline.length()) {
			int b, shift = 0, result = 0;
			do {
				b = polyline.charAt(index++) - 63;
				result |= (b & 0x1f) << shift;
				shift += 5;
			} while (b >= 0x20);
			int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
			lat += dlat;

			shift = 0;
			result = 0;
			do {
				b = polyline.charAt(index++) - 63;
				result |= (b & 0x1f) << shift;
				shift += 5;
			} while (b >= 0x20);
			int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
			lng += dlng;

			double latitude = lat / PRECISION;
			double longitude = lng / PRECISION;

			coordinates.add(Coordinate.builder()
				.lat(latitude)
				.lng(longitude)
				.build());
		}

		return coordinates;
	}

	/**
	 * Polyline 디코딩이 가능한지 확인합니다.
	 *
	 * @param polyline 확인할 polyline 문자열
	 * @return 디코딩 가능 여부
	 */
	public static boolean isValidPolyline(String polyline) {
		if (polyline == null || polyline.trim().isEmpty()) {
			return false;
		}

		try {
			// 간단한 유효성 검사: 모든 문자가 유효한 polyline 문자인지 확인
			for (char c : polyline.toCharArray()) {
				if (c < 63 || c > 126) {
					return false;
				}
			}
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Polyline의 대략적인 좌표 개수를 추정합니다.
	 *
	 * @param polyline polyline 문자열
	 * @return 추정 좌표 개수
	 */
	public static int estimateCoordinateCount(String polyline) {
		if (polyline == null || polyline.trim().isEmpty()) {
			return 0;
		}

		// polyline의 길이를 기반으로 대략적인 좌표 개수 추정
		// 일반적으로 한 좌표당 10-20자 정도의 문자를 사용
		return Math.max(1, polyline.length() / 15);
	}
}
