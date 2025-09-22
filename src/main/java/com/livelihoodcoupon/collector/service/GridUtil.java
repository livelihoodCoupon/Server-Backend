package com.livelihoodcoupon.collector.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 지리적 격자 시스템을 위한 유틸리티 클래스
 * 
 * <h3>주요 기능:</h3>
 * <ul>
 *   <li><b>경계 상자 계산:</b> 폴리곤의 최소/최대 좌표를 계산하여 경계 상자 생성</li>
 *   <li><b>점-폴리곤 포함 검사:</b> 특정 좌표가 폴리곤 내부에 있는지 판단</li>
 *   <li><b>격자 생성:</b> 지정된 영역을 격자로 분할하여 중심점 목록 생성</li>
 *   <li><b>격자 폴리곤 생성:</b> 격자 중심점과 반경을 기반으로 폴리곤 생성</li>
 * </ul>
 * 
 * <h3>사용 사례:</h3>
 * <ul>
 *   <li>대규모 지역을 작은 격자로 분할하여 효율적인 데이터 수집</li>
 *   <li>밀집도가 높은 지역의 재귀적 세분화</li>
 *   <li>지리적 범위 내 데이터 필터링</li>
 * </ul>
 */
public class GridUtil {

	/** 1미터당 위도/경도 차이 (대략적인 값) */
	public static final double DEGREE_PER_METER = 1.0 / 111_000.0;

	/**
	 * 폴리곤의 경계 상자(Bounding Box)를 계산합니다.
	 * 
	 * <p>주어진 폴리곤의 모든 점을 검사하여 최소/최대 위도와 경도를 찾아
	 * 경계 상자를 생성합니다. 이를 통해 격자 생성 시 필요한 범위를 효율적으로 계산할 수 있습니다.</p>
	 * 
	 * @param polygon 폴리곤 좌표 리스트 (각 점은 [경도, 위도] 형식)
	 * @return 경계 상자 정보 (최소/최대 위도, 경도)
	 */
	public static BoundingBox getBoundingBoxForPolygon(List<List<Double>> polygon) {
		if (polygon == null || polygon.isEmpty()) {
			return new BoundingBox(0, 0, 0, 0);
		}

		// 최소/최대 좌표 초기화
		double minLat = Double.MAX_VALUE;
		double maxLat = Double.MIN_VALUE;
		double minLng = Double.MAX_VALUE;
		double maxLng = Double.MIN_VALUE;

		// 모든 점을 순회하며 최소/최대 좌표 찾기
		for (List<Double> point : polygon) {
			double lng = point.get(0); // 경도 (X 좌표)
			double lat = point.get(1); // 위도 (Y 좌표)
			
			if (lat < minLat) minLat = lat;
			if (lat > maxLat) maxLat = lat;
			if (lng < minLng) minLng = lng;
			if (lng > maxLng) maxLng = lng;
		}
		return new BoundingBox(minLat, maxLat, minLng, maxLng);
	}

	/**
	 * 특정 좌표가 폴리곤 내부에 있는지 판단합니다.
	 * 
	 * <p>주어진 좌표가 폴리곤 내부에 있는지 판단합니다. 이를 통해 특정 좌표가 지정된 영역 내부에 있는지 판단할 수 있습니다.</p>
	 * 
	 * @param lat
	 * @param lng
	 * @param polygon
	 * @return
	 */
	public static boolean isPointInPolygon(double lat, double lng, List<List<Double>> polygon) {
		if (polygon == null || polygon.isEmpty()) {
			return false;
		}
		int i;
		int j;
		boolean isInside = false;
		int nvert = polygon.size();
		for (i = 0, j = nvert - 1; i < nvert; j = i++) {
			double vert_i_lng = polygon.get(i).get(0);
			double vert_i_lat = polygon.get(i).get(1);
			double vert_j_lng = polygon.get(j).get(0);
			double vert_j_lat = polygon.get(j).get(1);

			if (((vert_i_lat > lat) != (vert_j_lat > lat))
				&& (lng < (vert_j_lng - vert_i_lng) * (lat - vert_i_lat) / (vert_j_lat - vert_i_lat) + vert_i_lng)) {
				isInside = !isInside;
			}
		}
		return isInside;
	}

	/**
	 * 경계 상자를 기반으로 격자 중심점 목록을 생성합니다.
	 * 
	 * <p>주어진 경계 상자를 기반으로 격자 중심점 목록을 생성합니다. 이를 통해 지정된 영역을 격자로 분할하여 중심점 목록을 생성할 수 있습니다.</p>
	 * 
	 * @param latStart 경계 상자 시작 위도
	 * @param latEnd 경계 상자 종료 위도
	 * @param lngStart 경계 상자 시작 경도
	 * @param lngEnd 경계 상자 종료 경도
	 * @param gridCellRadiusMeters 격자 셀 반경 (미터)
	 * @return 격자 중심점 목록
	 */
	public static List<double[]> generateGridForBoundingBox(double latStart, double latEnd, double lngStart,
		double lngEnd, int gridCellRadiusMeters) {
		List<double[]> gridCenters = new ArrayList<>();

		double latStep = gridCellRadiusMeters * 2 * DEGREE_PER_METER;

		double midLat = (latStart + latEnd) / 2.0;
		double lngStep = gridCellRadiusMeters * 2 * DEGREE_PER_METER / Math.cos(Math.toRadians(midLat));

		for (double lat = latStart; lat <= latEnd; lat += latStep) {
			for (double lng = lngStart; lng <= lngEnd; lng += lngStep) {
				gridCenters.add(new double[] {lat + (latStep / 2), lng + (lngStep / 2)});
			}
		}
		return gridCenters;
	}

	/**
	 * 격자 중심점과 반경을 기반으로 폴리곤을 생성합니다.
	 * 
	 * <p>주어진 중심점과 반경을 기반으로 폴리곤을 생성합니다. 이를 통해 격자 중심점과 반경을 기반으로 폴리곤을 생성할 수 있습니다.</p>
	 * 
	 * @param centerLat 격자 중심점 위도
	 * @param centerLng 격자 중심점 경도
	 * @param radius 반경 (미터)
	 * @return 폴리곤
	 */
	public static List<List<Double>> createPolygonForCell(double centerLat, double centerLng, int radius) {
		double latOffset = radius * DEGREE_PER_METER;
		double lngOffset =
			radius * DEGREE_PER_METER / Math.cos(Math.toRadians(centerLat));
		double latStart = centerLat - latOffset;
		double latEnd = centerLat + latOffset;
		double lngStart = centerLng - lngOffset;
		double lngEnd = centerLng + lngOffset;
		return new ArrayList<>(Arrays.asList(Arrays.asList(lngStart, latStart), Arrays.asList(lngEnd, latStart),
			Arrays.asList(lngEnd, latEnd), Arrays.asList(lngStart, latEnd), Arrays.asList(lngStart, latStart)));
	}

	/**
	 * 경계 상자 정보를 담는 클래스
	 */
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class BoundingBox {
		private double latStart;
		private double latEnd;
		private double lngStart;
		private double lngEnd;
	}
}
