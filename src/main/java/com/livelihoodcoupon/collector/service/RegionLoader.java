package com.livelihoodcoupon.collector.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.livelihoodcoupon.collector.dto.GeoJsonFeature;
import com.livelihoodcoupon.collector.dto.GeoJsonFeatureCollection;
import com.livelihoodcoupon.collector.vo.RegionData;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class RegionLoader {

	private final ObjectMapper objectMapper;

	public RegionLoader(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public List<RegionData> loadRegions() {
		try {
			Map<String, String> sidoMap = loadSidoNames();

			try (InputStream sigIs = getClass().getResourceAsStream("/sig.json")) {
				if (sigIs == null) {
					throw new IOException("sig.json not found in classpath");
				}

				GeoJsonFeatureCollection sigFeatureCollection = objectMapper.readValue(sigIs,
					GeoJsonFeatureCollection.class);

				return sigFeatureCollection.getFeatures().stream()
					.map(feature -> convertFeatureToRegionData(feature, sidoMap))
					.collect(Collectors.toList());
			}

		} catch (IOException e) {
			throw new RuntimeException("Failed to load region data", e);
		}
	}

	private Map<String, String> loadSidoNames() throws IOException {
		try (InputStream sidoIs = getClass().getResourceAsStream("/sido.json")) {
			if (sidoIs == null) {
				throw new IOException("sido.json not found in classpath");
			}

			GeoJsonFeatureCollection sidoFeatureCollection = objectMapper.readValue(sidoIs,
				GeoJsonFeatureCollection.class);

			return sidoFeatureCollection.getFeatures().stream()
				.collect(Collectors.toMap(
					feature -> feature.getProperties().getCtprvnCd(),
					feature -> feature.getProperties().getSigKorNm(),
					(existing, replacement) -> existing // In case of duplicate keys
				));
		}
	}

	private RegionData convertFeatureToRegionData(GeoJsonFeature feature, Map<String, String> sidoMap) {
		RegionData regionData = new RegionData();

		String sigCd = feature.getProperties().getSigCd();
		String sigName = feature.getProperties().getSigKorNm();
		String sidoCd = sigCd.substring(0, 2);
		String sidoNameAbbr = sidoMap.get(sidoCd);

		if (sidoNameAbbr == null) {
			regionData.setName(sigName); // Fallback to just sigName if sido is not found
		} else {
			String sidoNameFull = expandSidoName(sidoNameAbbr);
			String fullName = sidoNameFull + " " + sigName;
			regionData.setName(fullName);
		}

		if (feature.getGeometry() != null && feature.getGeometry().getCoordinates() != null) {
			String type = feature.getGeometry().getType();
			Object rawCoordinates = feature.getGeometry().getCoordinates();

			try {
				if ("Polygon".equals(type)) {
					List<List<List<Double>>> polygon = objectMapper.convertValue(rawCoordinates, new TypeReference<>() {
					});
					regionData.setPolygons(List.of(polygon));
				} else if ("MultiPolygon".equals(type)) {
					List<List<List<List<Double>>>> multiPolygon = objectMapper.convertValue(rawCoordinates,
						new TypeReference<>() {
						});
					regionData.setPolygons(multiPolygon);
				}
			} catch (Exception e) {
				log.error("Could not parse coordinates for region: {}. Type: {}. Error: {}", regionData.getName(), type,
					e.getMessage());
				// Set polygons to null or empty list to indicate failure for this specific region
				regionData.setPolygons(null);
			}
		}

		return regionData;
	}

	private String expandSidoName(String name) {
		switch (name) {
			case "서울":
				return "서울특별시";
			case "부산":
				return "부산광역시";
			case "대구":
				return "대구광역시";
			case "인천":
				return "인천광역시";
			case "광주":
				return "광주광역시";
			case "대전":
				return "대전광역시";
			case "울산":
				return "울산광역시";
			case "세종":
				return "세종특별자치시";
			case "경기":
				return "경기도";
			case "강원":
				return "강원도";
			case "충북":
				return "충청북도";
			case "충남":
				return "충청남도";
			case "전북":
				return "전라북도";
			case "전남":
				return "전라남도";
			case "경북":
				return "경상북도";
			case "경남":
				return "경상남도";
			case "제주":
				return "제주특별자치도";
			default:
				return name;
		}
	}
}
