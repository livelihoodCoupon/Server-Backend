package com.livelihoodcoupon.collector.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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

import com.livelihoodcoupon.collector.dto.KakaoPlace;
import com.livelihoodcoupon.collector.dto.KakaoResponse;
import com.livelihoodcoupon.collector.entity.PlaceEntity;
import com.livelihoodcoupon.collector.entity.ScannedGrid;
import com.livelihoodcoupon.collector.repository.PlaceRepository;
import com.livelihoodcoupon.collector.repository.ScannedGridRepository;
import com.livelihoodcoupon.collector.vo.RegionData;
import com.livelihoodcoupon.common.service.MdcLogging;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CouponDataCollector {

	public static final String DEFAULT_KEYWORD = "소비쿠폰";
	private static final Logger log = LoggerFactory.getLogger(CouponDataCollector.class);
	private static final int INITIAL_GRID_RADIUS_METERS = 512;
	private static final int MAX_PAGE_PER_QUERY = 45;
	private static final int DENSE_AREA_THRESHOLD = 45;
	private static final int MAX_RECURSION_DEPTH = 7;
	private static final int API_CALL_DELAY_MS = 30; // Base delay
	private static final int MAX_RETRIES = 5; // Max retry attempts for 429 errors
	private static final long INITIAL_RETRY_DELAY_MS = 1000; // Initial delay for retry (1 second)

	private final KakaoApiService kakaoApiService;
	private final PlaceRepository placeRepository;
	private final ScannedGridRepository scannedGridRepository;
	private final CsvExportService csvExportService;
	private final GeoJsonExportService geoJsonExportService;
	private final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

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

		List<List<List<Double>>> currentLevelPolygons = new ArrayList<>();
		currentLevelPolygons.add(initialPolygon);
		int currentRadius = INITIAL_GRID_RADIUS_METERS;
		int depth = 0;

		while (!currentLevelPolygons.isEmpty() && depth < MAX_RECURSION_DEPTH) {
			log.info("    - [{}단계, 반경 {}m] 총 {}개 지역 병렬 탐색 시작...", depth, currentRadius, currentLevelPolygons.size());

			List<Callable<List<List<List<Double>>>>> tasks = new ArrayList<>();
			for (List<List<Double>> polygonToScan : currentLevelPolygons) {
				final int radiusForTask = currentRadius;
				Callable<List<List<List<Double>>>> task = () -> scanPolygon(region.getName(), DEFAULT_KEYWORD,
					polygonToScan, radiusForTask);
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

		log.info(">>> [ {} ] 지역, 키워드 [ {} ] 데이터 수집 및 DB 저장 완료.", region.getName(), DEFAULT_KEYWORD);

		// --- DB에 저장된 데이터를 기반으로 파일 생성 시작 ---
		log.info(">>> [ {} ] 지역 파일 생성을 시작합니다...", region.getName());
		csvExportService.exportSingleRegionToCsv(region.getName(), DEFAULT_KEYWORD);
		geoJsonExportService.exportSingleRegionToGeoJson(region.getName(), DEFAULT_KEYWORD);
		log.info(">>> [ {} ] 지역 파일 생성을 완료했습니다.", region.getName());
	}

	private List<List<List<Double>>> scanPolygon(String regionName, String keyword, List<List<Double>> polygon,
		int radius) {
		List<List<List<Double>>> denseSubPolygons = new ArrayList<>();
		GridUtil.BoundingBox bbox = GridUtil.getBoundingBoxForPolygon(polygon);
		List<double[]> gridCenters = GridUtil.generateGridForBoundingBox(bbox.getLatStart(),
			bbox.getLatEnd(), bbox.getLngStart(), bbox.getLngEnd(), radius);

		for (double[] center : gridCenters) {
			if (!GridUtil.isPointInPolygon(center[0], center[1], polygon)) {
				continue;
			}

			try {
				Optional<ScannedGrid> existingGrid = scannedGridRepository.findByRegionNameAndKeywordAndGridCenterLatAndGridCenterLngAndGridRadius(
					regionName, keyword, center[0], center[1], radius);

				if (existingGrid.isPresent()) {
					ScannedGrid.GridStatus status = existingGrid.get().getStatus();
					if (status == ScannedGrid.GridStatus.COMPLETED) {
						log.debug("    - [완료된 격자] 건너뛰기 (좌표: {},{})", center[0], center[1]);
						continue;
					}
					if (status == ScannedGrid.GridStatus.SUBDIVIDED) {
						log.debug("    - [분할된 격자] 하위 탐색 목록에 추가 (좌표: {},{})", center[0], center[1]);
						denseSubPolygons.add(GridUtil.createPolygonForCell(center[0], center[1], radius));
						continue;
					}
				}

				log.debug("    - [신규 격자] 밀집도 검사 API 호출 (좌표: {},{})", center[0], center[1]);
				KakaoResponse response = callKakaoApiWithRetry(
					() -> kakaoApiService.searchPlaces(keyword, center[1], center[0], radius, 1), "키워드 검색");
				if (response == null || response.getDocuments() == null)
					continue;

				int totalCount = response.getMeta().getTotal_count();

				if (totalCount > DENSE_AREA_THRESHOLD) {
					denseSubPolygons.add(GridUtil.createPolygonForCell(center[0], center[1], radius));
					scannedGridRepository.save(ScannedGrid.builder()
						.regionName(regionName).keyword(keyword).gridCenterLat(center[0]).gridCenterLng(center[1])
						.gridRadius(radius).status(ScannedGrid.GridStatus.SUBDIVIDED).build());
				} else {
					int foundCountInCell = savePaginatedPlaces(response, regionName, keyword, polygon, center, radius);
					scannedGridRepository.save(ScannedGrid.builder()
						.regionName(regionName).keyword(keyword).gridCenterLat(center[0]).gridCenterLng(center[1])
						.gridRadius(radius).status(ScannedGrid.GridStatus.COMPLETED).build());
					if (foundCountInCell > 0) {
						log.info("        - 일반 지역 (결과: {}개). {}개의 새 장소를 DB에 저장.", totalCount, foundCountInCell);
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
		GridUtil.BoundingBox bbox = GridUtil.getBoundingBoxForPolygon(polygon);
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
					log.info("        - 페이지 {}에서 {}개의 새 장소를 DB에 저장 시도.", currentPage, savedInPage);
				}
				foundCount += savedInPage;

				if (currentResponse.getMeta().is_end())
					break;

				currentPage++;
				if (currentPage > MAX_PAGE_PER_QUERY)
					break;

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
		List<PlaceEntity> placeEntities = new ArrayList<>();
		for (KakaoPlace place : places) {
			double lat = Double.parseDouble(place.getY());
			double lng = Double.parseDouble(place.getX());

			if (!GridUtil.isPointInPolygon(lat, lng, regionPolygon)) {
				continue;
			}

			PlaceEntity entity = PlaceEntity.builder()
				.placeId(place.getId())
				.placeName(place.getPlaceName())
				.category(place.getCategoryName())
				.categoryGroupCode(place.getCategoryGroupCode())
				.categoryGroupName(place.getCategoryGroupName())
				.phone(place.getPhone())
				.lotAddress(place.getAddressName())
				.roadAddress(place.getRoadAddressName())
				.lng(lng)
				.lat(lat)
				.placeUrl(place.getPlaceUrl())
				.region(regionName)
				.keyword(keyword)
				.build();
			placeEntities.add(entity);
		}

		if (placeEntities.isEmpty()) {
			return 0;
		}

		try {
			placeRepository.saveAll(placeEntities);
			return placeEntities.size();
		} catch (DataIntegrityViolationException e) {
			log.warn("    - 데이터 저장 중 무결성 제약 조건 위반 발생 (예: 중복 데이터). 건너뜁니다.");
			return 0;
		}
	}

	private <T> T callKakaoApiWithRetry(Callable<T> apiCall, String errorMessage) throws Exception {
		int attempts = 0;
		long currentDelay = API_CALL_DELAY_MS;

		while (attempts < MAX_RETRIES) {
			try {
				Thread.sleep(currentDelay);
				return apiCall.call();
			} catch (WebClientResponseException e) {
				if (e.getStatusCode().value() == 429) {
					attempts++;
					log.warn("    - API 호출 429 오류 발생 (재시도 {}/{}) - {}", attempts, MAX_RETRIES, errorMessage);
					currentDelay = (long)(INITIAL_RETRY_DELAY_MS * Math.pow(2, attempts - 1));
					if (currentDelay > 60000)
						currentDelay = 60000;
					log.warn("    - 다음 재시도까지 {}ms 대기...", currentDelay);
				} else {
					throw e;
				}
			} catch (Exception e) {
				throw e;
			}
		}
		throw new RuntimeException("API 호출 최대 재시도 횟수 초과: " + errorMessage);
	}
}