package com.livelihoodcoupon.collector.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.livelihoodcoupon.collector.dto.RegionKeywordDto;
import com.livelihoodcoupon.collector.entity.ScannedGrid;
import com.livelihoodcoupon.collector.repository.ScannedGridRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GeoJsonExportService {

	private static final Logger log = LoggerFactory.getLogger(GeoJsonExportService.class);
	private final ScannedGridRepository scannedGridRepository;
	private final ObjectMapper objectMapper;

	@Transactional(readOnly = true)
	public void exportAllRegionsToGeoJson() {
		log.info("전체 지역의 격자 데이터 GeoJSON 파일로 내보내기 작업을 시작합니다.");
		List<RegionKeywordDto> regionKeywords = scannedGridRepository.findDistinctRegionAndKeyword();
		if (regionKeywords.isEmpty()) {
			log.info("내보낼 격자 데이터가 DB에 없습니다.");
			return;
		}

		log.info("총 {}개 (지역, 키워드) 조합에 대한 GeoJSON 파일 생성을 시작합니다.", regionKeywords.size());
		for (RegionKeywordDto dto : regionKeywords) {
			exportSingleRegionToGeoJson(dto.getRegionName(), dto.getKeyword());
		}
		log.info("전체 지역 격자 데이터 GeoJSON 파일 내보내기 작업을 완료했습니다.");
	}

	@Transactional(readOnly = true)
	public void exportSingleRegionToGeoJson(String regionName, String keyword) {
		String filename = String.format("data/geojson/%s_%s_grid_data.geojson",
			regionName.replace(" ", "_"),
			keyword.replace(" ", "_"));

		log.info("[{}-{}] GeoJSON 파일 저장을 시작합니다. 파일명: {}", regionName, keyword, filename);

		new File("data/geojson").mkdirs();

		Map<String, Object> geoJson = new LinkedHashMap<>();
		geoJson.put("type", "FeatureCollection");
		List<Map<String, Object>> features = new ArrayList<>();

		try (Stream<ScannedGrid> gridStream = scannedGridRepository.findByRegionNameAndKeyword(regionName, keyword)) {
			gridStream.forEach(cell -> {
				Map<String, Object> feature = new LinkedHashMap<>();
				feature.put("type", "Feature");

				Map<String, Object> geometry = new LinkedHashMap<>();
				geometry.put("type", "Polygon");
				geometry.put("coordinates", Collections.singletonList(
					GridUtil.createPolygonForCell(cell.getGridCenterLat(), cell.getGridCenterLng(),
						cell.getGridRadius()))
				);
				feature.put("geometry", geometry);

				Map<String, Object> properties = new LinkedHashMap<>();
				properties.put("radius", cell.getGridRadius());
				properties.put("status", cell.getStatus().toString());
				properties.put("keyword", cell.getKeyword());
				feature.put("properties", properties);

				features.add(feature);
			});

			geoJson.put("features", features);

			objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(filename), geoJson);
			log.info("[{}-{}] GeoJSON 파일 저장을 완료했습니다. (총 {}개 격자)", regionName, keyword, features.size());

		} catch (IOException e) {
			log.error("[{}-{}] GeoJSON 파일 저장 중 오류가 발생했습니다: {}", regionName, keyword, e.getMessage());
		}
	}
}