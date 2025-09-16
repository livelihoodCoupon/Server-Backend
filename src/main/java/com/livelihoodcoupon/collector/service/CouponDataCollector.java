package com.livelihoodcoupon.collector.service;

import java.io.File;
import java.io.IOException;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import jakarta.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.livelihoodcoupon.collector.dto.Coord2RegionCodeResponse;
import com.livelihoodcoupon.collector.dto.KakaoPlace;
import com.livelihoodcoupon.collector.dto.KakaoResponse;

import com.livelihoodcoupon.collector.vo.GridCellInfo;
import com.livelihoodcoupon.collector.vo.RegionData;
import com.livelihoodcoupon.common.service.MdcLogging;

@Service
public class CouponDataCollector {

	private static final Logger log = LoggerFactory.getLogger(CouponDataCollector.class);
	private static final String DEFAULT_KEYWORD = "소비쿠폰";
	private static final int INITIAL_GRID_RADIUS_METERS = 512;
	private static final int MAX_PAGE_PER_QUERY = 45;
	private static final int DENSE_AREA_THRESHOLD = 45;
	private static final int MAX_RECURSION_DEPTH = 7;
	private static final int API_CALL_DELAY_MS = 30; // Base delay
	private static final int MAX_RETRIES = 5; // Max retry attempts for 429 errors
	private static final long INITIAL_RETRY_DELAY_MS = 1000; // Initial delay for retry (1 second)
	private final KakaoApiService kakaoApiService;
	
	private final ObjectMapper objectMapper;
	private final ExecutorService executor;
	private List<GridCellInfo> collectedGridCells;
	private List<KakaoPlace> collectedPlaces;

	public CouponDataCollector(KakaoApiService kakaoApiService,
		ObjectMapper objectMapper) {
		this.kakaoApiService = kakaoApiService;
		this.objectMapper = objectMapper;
		this.executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
	}

	@PreDestroy
	public void shutdownExecutor() {
		log.info("Shutting down executor service...");
		executor.shutdown();
	}

	public void collectForRegions(List<RegionData> regions) {
		try (MdcLogging.MdcContext ignored = MdcLogging.withContext("traceId", UUID.randomUUID().toString())) {
			log.info("======== 전체 지역, 키워드 [{}] 데이터 수집 시작 ========", DEFAULT_KEYWORD);
			for (RegionData region : regions) {
				collectForSingleRegion(region);
			}
			log.info("======== 전체 지역, 키워드 [{}] 데이터 수집 완료 ========", DEFAULT_KEYWORD);
		}
	}

	public void collectForSingleRegion(RegionData region) {
		List<List<Double>> initialPolygon = region.getPolygon();
		if (initialPolygon == null || initialPolygon.isEmpty()) {
			log.warn("    - 경고: [ {} ] 지역에 폴리곤(polygon)이 정의되지 않아 건너뜁니다.", region.getName());
			return;
		}

		log.info(">>> [ {} ] 지역, 키워드 [ {} ]로 데이터 수집을 시작합니다.", region.getName(), DEFAULT_KEYWORD);
		new File("data/csv").mkdirs(); // Ensure data/csv directory exists
		this.collectedGridCells = Collections.synchronizedList(new ArrayList<>());
		this.collectedPlaces = Collections.synchronizedList(new ArrayList<>());

		List<List<List<Double>>> currentLevelPolygons = new ArrayList<>();
		currentLevelPolygons.add(initialPolygon);
		int currentRadius = INITIAL_GRID_RADIUS_METERS;
		int depth = 0;

		while (!currentLevelPolygons.isEmpty() && depth < MAX_RECURSION_DEPTH) {
			log.info("    - [{}단계, 반경 {}m] 총 {}개 지역 병렬 탐색 시작...", depth, currentRadius, currentLevelPolygons.size());

			List<Callable<List<List<List<Double>>>>> tasks = new ArrayList<>();
			for (List<List<Double>> polygonToScan : currentLevelPolygons) {
				final int radiusForTask = currentRadius;
				final int currentDepth = depth;
				Callable<List<List<List<Double>>>> task = () -> scanPolygon(region.getName(), DEFAULT_KEYWORD,
					polygonToScan, radiusForTask, currentDepth);
				tasks.add(task);
			}

			List<List<List<Double>>> nextLevelPolygons = new CopyOnWriteArrayList<>();
			try {
				List<Future<List<List<List<Double>>>>> futures = executor.invokeAll(tasks);
				for (Future<List<List<List<Double>>>> future : futures) {
					nextLevelPolygons.addAll(future.get());
				}
			} catch (InterruptedException | ExecutionException e) {
				log.error("Parallel collection failed at depth {}", depth, e);
				Thread.currentThread().interrupt();
				break;
			}

			currentLevelPolygons = new ArrayList<>(nextLevelPolygons);
			currentRadius /= 2;
			depth++;
		}

		if (!currentLevelPolygons.isEmpty() && depth >= MAX_RECURSION_DEPTH) {
			log.warn("    - 최대 재귀 깊이({})에 도달하여 마지막 {}개 지역 강제 수집 시작...", depth, currentLevelPolygons.size());
			forceCollectInParallel(region.getName(), DEFAULT_KEYWORD, currentLevelPolygons, currentRadius);
		}

		log.info(">>> [ {} ] 지역, 키워드 [ {} ] 데이터 수집 완료.", region.getName(), DEFAULT_KEYWORD);

		String geojsonFilename = "data/geojson/" + region.getName().replace(" ", "_") + "_grid_data.geojson";
		exportGridDataToGeoJson(collectedGridCells, geojsonFilename);
		log.info("    - 격자 데이터 GeoJSON 파일로 저장 완료: {}", geojsonFilename);

		String csvFilename = "data/csv/" + region.getName().replace(" ", "_") + "_places_data.csv";
		exportPlacesToCsv(collectedPlaces, csvFilename, region.getName(), DEFAULT_KEYWORD);
		log.info("    - 장소 데이터 CSV 파일로 저장 완료: {}", csvFilename);
	}

	private List<List<List<Double>>> scanPolygon(String regionName, String keyword, List<List<Double>> polygon,
		int radius, int depth) {
		List<List<List<Double>>> denseSubPolygons = new ArrayList<>();
		GridUtil.BoundingBox bbox = GridUtil.getBoundingBoxForPolygon(
			polygon);
		List<double[]> gridCenters = GridUtil.generateGridForBoundingBox(bbox.getLatStart(),
			bbox.getLatEnd(), bbox.getLngStart(), bbox.getLngEnd(), radius);

		for (double[] center : gridCenters) {
			if (!GridUtil.isPointInPolygon(center[0], center[1], polygon)) {
				continue;
			}

			try {
				// Use retry logic for API calls
				KakaoResponse response = callKakaoApiWithRetry(
					() -> kakaoApiService.searchPlaces(keyword, center[1], center[0], radius, 1), "키워드 검색");
				if (response == null || response.getDocuments() == null)
					continue;

				int totalCount = response.getMeta().getTotal_count();
				int pageableCount = response.getMeta().getPageable_count();

				if (totalCount > DENSE_AREA_THRESHOLD) {
					denseSubPolygons.add(createPolygonForCell(center[0], center[1], radius));
					collectedGridCells.add(
						new GridCellInfo(center[0], center[1], radius, depth, totalCount, pageableCount, true));
				} else {
					int foundCountInCell = savePaginatedPlaces(response, regionName, keyword, polygon, center, radius);
					collectedGridCells.add(
						new GridCellInfo(center[0], center[1], radius, depth, totalCount, pageableCount, false));
					if (foundCountInCell > 0) {
						log.info("        - 일반 지역 (결과: {}개). {}개의 새 장소를 저장.", totalCount, foundCountInCell);
					}
				}
			} catch (Exception e) {
				if (e instanceof InterruptedException)
					Thread.currentThread().interrupt();
				log.error("    - 격자 수집 중 오류 발생 (좌표: {},{}): {}", center[0], center[1], e.getMessage());
			}
		}
		return denseSubPolygons;
	}

	private void forceCollectInParallel(String regionName, String keyword, List<List<List<Double>>> polygons,
		int radius) {
		List<Callable<Void>> tasks = new ArrayList<>();
		for (List<List<Double>> polygon : polygons) {
			tasks.add(() -> {
				forceCollectAtMaxDepth(regionName, keyword, polygon, radius);
				return null;
			});
		}
		try {
			executor.invokeAll(tasks);
		} catch (InterruptedException e) {
			log.error("Parallel force collection failed", e);
			Thread.currentThread().interrupt();
		}
	}

	private void forceCollectAtMaxDepth(String regionName, String keyword, List<List<Double>> polygon, int radius) {
		GridUtil.BoundingBox bbox = GridUtil.getBoundingBoxForPolygon(
			polygon);
		List<double[]> gridCenters = GridUtil.generateGridForBoundingBox(bbox.getLatStart(),
			bbox.getLatEnd(), bbox.getLngStart(), bbox.getLngEnd(), radius);

		for (double[] center : gridCenters) {
			if (!GridUtil.isPointInPolygon(center[0], center[1], polygon))
				continue;
			savePaginatedPlaces(null, regionName, keyword, polygon, center, radius);
		}
	}

	private int savePaginatedPlaces(KakaoResponse firstPageResponse, String regionName, String keyword,
		List<List<Double>> regionPolygon, double[] center, int radius) {
		int foundCount = 0;
		try {
			KakaoResponse currentResponse = firstPageResponse;
			int currentPage = 1;

			if (currentResponse == null) {
				final int pageForFirstCall = currentPage;
				currentResponse = callKakaoApiWithRetry(
					() -> kakaoApiService.searchPlaces(keyword, center[1], center[0], radius, pageForFirstCall),
					"페이지네이션 검색 (1페이지)");
			}

			while (currentPage <= MAX_PAGE_PER_QUERY) {
				if (currentResponse == null || currentResponse.getDocuments() == null || currentResponse.getDocuments()
					.isEmpty()) {
					break;
				}

				int savedInPage = savePlaces(currentResponse.getDocuments(), regionName, keyword, regionPolygon);
				if (savedInPage > 0) {
					log.info("        - 페이지 {}에서 {}개의 새 장소를 저장.", currentPage, savedInPage);
				}
				foundCount += savedInPage;

				if (currentResponse.getMeta().is_end()) {
					break;
				}

				currentPage++;
				if (currentPage > MAX_PAGE_PER_QUERY) {
					break;
				}

				final int pageForNextCall = currentPage;
				currentResponse = callKakaoApiWithRetry(
					() -> kakaoApiService.searchPlaces(keyword, center[1], center[0], radius, pageForNextCall),
					"페이지네이션 검색 ({}페이지)".formatted(currentPage));
			}
		} catch (Exception e) {
			if (e instanceof InterruptedException)
				Thread.currentThread().interrupt();
			log.error("    - 페이지네이션 수집 중 오류 발생: {}", e.getMessage());
		}
		return foundCount;
	}

	private int savePlaces(List<KakaoPlace> places, String regionName, String keyword,
		List<List<Double>> regionPolygon) {
		int count = 0;
		for (KakaoPlace place : places) {
			double lat = Double.parseDouble(place.getY());
			double lng = Double.parseDouble(place.getX());

			if (!GridUtil.isPointInPolygon(lat, lng, regionPolygon))
				continue;

			// Add to collectedPlaces instead of saving to DB
			collectedPlaces.add(place);
			count++;
		}
		return count;
	}

	private List<List<Double>> createPolygonForCell(double centerLat, double centerLng, int radius) {
		double latOffset = radius * GridUtil.DEGREE_PER_METER;
		double lngOffset =
			radius * GridUtil.DEGREE_PER_METER / Math.cos(Math.toRadians(centerLat));
		double latStart = centerLat - latOffset, latEnd = centerLat + latOffset;
		double lngStart = centerLng - lngOffset, lngEnd = centerLng + lngOffset;
		return new ArrayList<>(Arrays.asList(Arrays.asList(lngStart, latStart), Arrays.asList(lngEnd, latStart),
			Arrays.asList(lngEnd, latEnd), Arrays.asList(lngStart, latEnd), Arrays.asList(lngStart, latStart)));
	}

	private String getActualRegionName(KakaoPlace place) {
		try {
			return callKakaoApiWithRetry(() -> {
				Coord2RegionCodeResponse regionInfo = kakaoApiService.getRegionInfo(Double.parseDouble(place.getX()),
					Double.parseDouble(place.getY()));
				if (regionInfo != null && regionInfo.getDocuments() != null && !regionInfo.getDocuments().isEmpty()) {
					return regionInfo.getDocuments()
						.stream()
						.filter(doc -> "B".equals(doc.getRegionType()))
						.findFirst()
						.map(Coord2RegionCodeResponse.RegionDocument::getAddressName)
						.orElse(null);
				}
				return null;
			}, "지역 정보 조회");
		} catch (Exception regionEx) {
			if (regionEx instanceof InterruptedException)
				Thread.currentThread().interrupt();
			log.error("        - 장소({})의 실제 지역 정보 조회 중 오류 발생: {}", place.getPlaceName(), regionEx.getMessage());
		}
		return null;
	}

	// Helper method to call Kakao API with retry logic
	private <T> T callKakaoApiWithRetry(Callable<T> apiCall, String errorMessage) throws Exception {
		int attempts = 0;
		long currentDelay = API_CALL_DELAY_MS; // Use the base delay for the first attempt

		while (attempts < MAX_RETRIES) {
			try {
				Thread.sleep(currentDelay); // Apply delay before each attempt
				return apiCall.call(); // Execute the actual API call
			} catch (WebClientResponseException e) { // Catch specific WebClient errors
				if (e.getStatusCode().value() == 429) {
					attempts++;
					log.warn("    - API 호출 429 오류 발생 (재시도 {}/{}) - {}", attempts, MAX_RETRIES, errorMessage);
					currentDelay = (long)(INITIAL_RETRY_DELAY_MS * Math.pow(2, attempts - 1)); // Exponential backoff
					if (currentDelay > 60000)
						currentDelay = 60000; // Cap max delay at 1 minute
					log.warn("    - 다음 재시도까지 {}ms 대기...", currentDelay);
				} else {
					throw e; // Re-throw other WebClient errors
				}
			} catch (Exception e) { // Catch other exceptions (InterruptedException, etc.)
				throw e; // Re-throw
			}
		}
		throw new RuntimeException("API 호출 최대 재시도 횟수 초과: " + errorMessage);
	}

	private void exportPlacesToCsv(List<KakaoPlace> places, String filename, String regionName, String keyword) {
		log.info("    - 장소 데이터 CSV 파일로 저장 시작: {}", filename);
		try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
			// Write CSV header
			writer.println("placeId,region,placeName,roadAddress,lotAddress,lat,lng,phone,category,keyword,categoryGroupCode,categoryGroupName,placeUrl,distance");

			// Write place data
			for (KakaoPlace place : places) {
				// Need to get actualRegionName for each place
				String actualRegionName = getActualRegionName(place);
				writer.printf("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",%s,%s,\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"\n",
					place.getId(),
					actualRegionName != null ? actualRegionName : regionName, // Use actualRegionName if available
					place.getPlaceName(),
					place.getRoadAddressName(),
					place.getAddressName(),
					place.getY(), // lat
					place.getX(), // lng
					place.getPhone(),
					place.getCategoryName(),
					keyword, // The keyword used for collection
					place.getCategoryGroupCode(),
					place.getCategoryGroupName(),
					place.getPlaceUrl(),
					place.getDistance()
				);
			}
			log.info("    - 장소 데이터 CSV 파일 저장 완료: {}", filename);
		} catch (IOException e) {
			log.error("CSV 파일 저장 중 오류 발생: {}", e.getMessage());
		}
	}

	private void exportGridDataToGeoJson(List<GridCellInfo> gridCells, String filename) {
		Map<String, Object> geoJson = new LinkedHashMap<>();
		geoJson.put("type", "FeatureCollection");
		List<Map<String, Object>> features = new ArrayList<>();
		for (GridCellInfo cell : gridCells) {
			Map<String, Object> feature = new LinkedHashMap<>();
			feature.put("type", "Feature");
			Map<String, Object> geometry = new LinkedHashMap<>();
			geometry.put("type", "Polygon");
			geometry.put("coordinates",
				Collections.singletonList(createPolygonForCell(cell.getLat(), cell.getLng(), cell.getRadius())));
			feature.put("geometry", geometry);
			Map<String, Object> properties = new LinkedHashMap<>();
			properties.put("radius", cell.getRadius());
			properties.put("recursionDepth", cell.getRecursionDepth());
			properties.put("totalCount", cell.getTotalCount());
			properties.put("pageableCount", cell.getPageableCount());
			properties.put("isDense", cell.isDense());
			feature.put("properties", properties);
			features.add(feature);
		}
		geoJson.put("features", features);
		try {
			objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(filename), geoJson);
		} catch (IOException e) {
			log.error("GeoJSON 파일 저장 중 오류 발생: {}", e.getMessage());
		}
	}
}
