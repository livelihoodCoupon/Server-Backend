package com.livelihoodcoupon.collector.service;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class GridUtil {

	public static final double DEGREE_PER_METER = 1.0 / 111_000.0;

	public static BoundingBox getBoundingBoxForPolygon(List<List<Double>> polygon) {
		if (polygon == null || polygon.isEmpty()) {
			return new BoundingBox(0, 0, 0, 0);
		}

		double minLat = Double.MAX_VALUE;
		double maxLat = Double.MIN_VALUE;
		double minLng = Double.MAX_VALUE;
		double maxLng = Double.MIN_VALUE;

		for (List<Double> point : polygon) {
			double lng = point.get(0);
			double lat = point.get(1);
			if (lat < minLat)
				minLat = lat;
			if (lat > maxLat)
				maxLat = lat;
			if (lng < minLng)
				minLng = lng;
			if (lng > maxLng)
				maxLng = lng;
		}
		return new BoundingBox(minLat, maxLat, minLng, maxLng);
	}

	public static boolean isPointInPolygon(double lat, double lng, List<List<Double>> polygon) {
		if (polygon == null || polygon.isEmpty()) {
			return false;
		}
		int i, j;
		boolean isInside = false;
		int nvert = polygon.size();
		for (i = 0, j = nvert - 1; i < nvert; j = i++) {
			double vert_i_lng = polygon.get(i).get(0);
			double vert_i_lat = polygon.get(i).get(1);
			double vert_j_lng = polygon.get(j).get(0);
			double vert_j_lat = polygon.get(j).get(1);

			if (((vert_i_lat > lat) != (vert_j_lat > lat)) &&
				(lng < (vert_j_lng - vert_i_lng) * (lat - vert_i_lat) / (vert_j_lat - vert_i_lat) + vert_i_lng)) {
				isInside = !isInside;
			}
		}
		return isInside;
	}

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