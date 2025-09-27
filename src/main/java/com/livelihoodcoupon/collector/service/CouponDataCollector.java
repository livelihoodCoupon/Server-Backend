package com.livelihoodcoupon.collector.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import jakarta.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.livelihoodcoupon.collector.dto.KakaoPlace;
import com.livelihoodcoupon.collector.dto.KakaoResponse;
import com.livelihoodcoupon.collector.entity.PlaceEntity;
import com.livelihoodcoupon.collector.entity.ScannedGrid;
import com.livelihoodcoupon.collector.repository.CollectorPlaceRepository;
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
	private static final int SMALL_REGION_GRID_RADIUS_METERS = 256;
	private static final int SMALL_REGION_THRESHOLD_METERS = 7500; // 7.5km
	private static final int MAX_PAGE_PER_QUERY = 45;
	private static final int DENSE_AREA_THRESHOLD = 45;
	private static final int MAX_RECURSION_DEPTH = 9;
	private static final int API_CALL_DELAY_MS = 30; // Base delay
	private static final int MAX_RETRIES = 5; // Max retry attempts for 429 errors
	private static final long INITIAL_RETRY_DELAY_MS = 1000; // Initial delay for retry (1 second)

	private final KakaoApiService kakaoApiService;
	private final CollectorPlaceRepository collectorPlaceRepository;
	private final ScannedGridRepository scannedGridRepository;
	private final CsvExportService csvExportService;
	private final GeoJsonExportService geoJsonExportService;
	private final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

	@PreDestroy
	public void shutdownExecutor() {
		log.info("Shutting down executor service...");
		executor.shutdown();
	}

	/**
	 * 여러 지역에 대한 소비쿠폰 데이터 수집을 시작합니다.
	 *
	 * <p>전국 또는 여러 지역의 소비쿠폰 장소 데이터를 순차적으로 수집합니다.
	 * 각 지역별로 독립적인 수집 프로세스를 실행하며, MDC 로깅을 통해 추적 가능합니다.</p>
	 *
	 * @param regions 수집할 지역 목록 (RegionData 리스트)
	 */
	public void collectForRegions(List<RegionData> regions) {
		try (MdcLogging.MdcContext ignored = MdcLogging.withContext("traceId", UUID.randomUUID().toString())) {
			log.info("======== 전체 지역, 키워드 [{}] 데이터 수집 시작 ========", DEFAULT_KEYWORD);
			// 각 지역별로 순차적으로 데이터 수집 실행
			for (RegionData region : regions) {
				collectForSingleRegion(region);
			}
			log.info("======== 전체 지역, 키워드 [{}] 데이터 수집 완료 ========", DEFAULT_KEYWORD);
		}
	}

	/**
	 * 단일 지역에 대한 소비쿠폰 데이터 수집을 실행합니다.
	 *
	 * <p>지정된 지역의 폴리곤을 기반으로 격자 시스템을 구축하고, 각 격자별로 카카오 API를 호출하여
	 * 소비쿠폰 장소 데이터를 수집합니다. 밀집도가 높은 지역은 재귀적으로 세분화하여 수집합니다.</p>
	 *
	 * <h3>수집 프로세스:</h3>
	 * <ol>
	 *   <li>지역 폴리곤 유효성 검사</li>
	 *   <li>초기 격자 생성 및 밀집도 검사</li>
	 *   <li>밀집 지역 재귀적 세분화 (최대 5단계)</li>
	 *   <li>수집된 데이터 DB 저장</li>
	 *   <li>CSV 및 GeoJSON 파일 생성</li>
	 * </ol>
	 *
	 * @param region 수집할 지역 정보 (폴리곤 좌표 포함)
	 */
	public void collectForSingleRegion(RegionData region) {
		long startTime = System.currentTimeMillis();

		// 1. 지역 폴리곤 유효성 검사
		List<List<List<List<Double>>>> multiPolygon = region.getPolygons();
		if (multiPolygon == null || multiPolygon.isEmpty()) {
			log.warn("    - 경고: [ {} ] 지역에 폴리곤(polygon)이 정의되지 않아 건너뜁니다.", region.getName());
			return;
		}

		log.info(">>> [ {} ] 지역, 키워드 [ {} ]로 데이터 수집을 시작합니다.", region.getName(), DEFAULT_KEYWORD);

		// 중복 방지를 위한 발견된 장소 ID 집합
		Set<String> foundPlaceIds = ConcurrentHashMap.newKeySet();

		// 2. 초기 격자 생성 및 밀집도 검사
		List<List<List<Double>>> initialRings = new ArrayList<>();
		for (List<List<List<Double>>> polygon : multiPolygon) {
			if (polygon != null && !polygon.isEmpty()) {
				initialRings.add(polygon.get(0));
			}
		}

		if (initialRings.isEmpty()) {
			log.warn("    - 경고: [ {} ] 지역에 유효한 폴리곤 데이터가 없습니다.", region.getName());
			return;
		}

		for (List<List<Double>> ring : initialRings) {
			GridUtil.BoundingBox bbox = GridUtil.getBoundingBoxForPolygon(ring);
			double bboxHeight = (bbox.getLatEnd() - bbox.getLatStart()) * 111000;
			double bboxWidth =
				(bbox.getLngEnd() - bbox.getLngStart()) * 111000 * Math.cos(Math.toRadians(bbox.getLatStart()));

			int initialRadius = (Math.max(bboxWidth, bboxHeight) < SMALL_REGION_THRESHOLD_METERS)
				? SMALL_REGION_GRID_RADIUS_METERS
				: INITIAL_GRID_RADIUS_METERS;

			if (initialRadius != INITIAL_GRID_RADIUS_METERS) {
				log.info("    - [ {} ] 지역의 일부가 작아, 격자 크기를 {}m로 조정합니다.", region.getName(), initialRadius);
			}

			scanAndCollectForPolygon(ring, initialRadius, foundPlaceIds, region.getName());
		}

		log.info(">>> [ {} ] 지역, 키워드 [ {} ] 데이터 수집 및 DB 저장 완료.", region.getName(), DEFAULT_KEYWORD);

		log.info(">>> [ {} ] 지역 파일 생성을 시작합니다...", region.getName());
		csvExportService.exportSingleRegionToCsv(region.getName(), DEFAULT_KEYWORD);
		geoJsonExportService.exportSingleRegionToGeoJson(region.getName(), DEFAULT_KEYWORD);
		log.info(">>> [ {} ] 지역 파일 생성을 완료했습니다.", region.getName());

		long endTime = System.currentTimeMillis();
		log.info(">>> [ {} ] 지역 수집 완료. 총 소요 시간: {}ms", region.getName(), (endTime - startTime));
	}

	protected void scanAndCollectForPolygon(List<List<Double>> polygon, int initialRadius, Set<String> foundPlaceIds,
		String regionName) {
		List<List<List<Double>>> currentLevelPolygons = new ArrayList<>();
		currentLevelPolygons.add(polygon);
		int currentRadius = initialRadius;
		int depth = 0;

		// 3. 밀집 지역 재귀적 세분화 (최대 5단계) & 4. 수집된 데이터 DB 저장
		while (!currentLevelPolygons.isEmpty() && depth < MAX_RECURSION_DEPTH) {
			log.info("    - [{}단계, 반경 {}m] 총 {}개 지역 병렬 탐색 시작...", depth, currentRadius, currentLevelPolygons.size());

			// 각 격자별 처리
			List<Callable<List<List<List<Double>>>>> tasks = new ArrayList<>();
			for (List<List<Double>> polygonToScan : currentLevelPolygons) {
				final int radiusForTask = currentRadius;
				Callable<List<List<List<Double>>>> task = () -> scanPolygon(regionName, DEFAULT_KEYWORD,
					polygonToScan, radiusForTask, foundPlaceIds);
				tasks.add(task);
			}

			// 다음 단계 격자 생성
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
			forceCollectInParallel(regionName, DEFAULT_KEYWORD, currentLevelPolygons, currentRadius,
				foundPlaceIds);
		}
	}

	/**
	 * 폴리곤 영역을 격자로 나누어 소비쿠폰 장소 데이터를 수집합니다.
	 *
	 * <p>지정된 폴리곤을 격자로 분할하고, 각 격자별로 카카오 API를 호출하여 소비쿠폰 장소를 검색합니다.
	 * 밀집도가 높은 격자는 하위 폴리곤으로 분할하여 재귀적으로 처리합니다.</p>
	 *
	 * @param regionName 지역명
	 * @param keyword 검색 키워드
	 * @param polygon 검색할 폴리곤 좌표
	 * @param radius 격자 반경 (미터)
	 * @param foundPlaceIds 중복 방지를 위한 발견된 장소 ID 집합
	 * @return 밀집도가 높아 재분할이 필요한 하위 폴리곤 목록
	 */
	private List<List<List<Double>>> scanPolygon(String regionName, String keyword, List<List<Double>> polygon,
		int radius, Set<String> foundPlaceIds) {
		List<List<List<Double>>> denseSubPolygons = new ArrayList<>();

		// 1. 폴리곤의 경계 상자(Bounding Box) 계산
		GridUtil.BoundingBox bbox = GridUtil.getBoundingBoxForPolygon(polygon);

		// 2. 경계 상자 내에서 격자 중심점들 생성
		List<double[]> gridCenters = GridUtil.generateGridForBoundingBox(bbox.getLatStart(),
			bbox.getLatEnd(), bbox.getLngStart(), bbox.getLngEnd(), radius);

		// 3. 각 격자 중심점에 대해 데이터 수집 수행
		for (double[] center : gridCenters) {
			// 3-1. 격자 중심점이 폴리곤 내부에 있는지 확인 (없으면 넘어감)
			if (GridUtil.isPointNotInPolygon(center[0], center[1], polygon)) {
				continue;
			}

			try {
				// 3-2. Redis 캐싱을 사용한 격자 상태 확인: 이미 처리된 격자인지 확인 (중복 처리 방지)
				ScannedGrid cachedGrid = getCachedGrid(regionName, keyword, center[0], center[1], radius);
				if (cachedGrid != null) {
					ScannedGrid.GridStatus status = cachedGrid.getStatus();
					if (status == ScannedGrid.GridStatus.COMPLETED) {
						// 이미 완료된 격자는 넘어감
						log.debug("    - [캐시된 완료 격자] 건너뛰기 (좌표: {},{})", center[0], center[1]);
						continue;
					}
					if (status == ScannedGrid.GridStatus.SUBDIVIDED) {
						// 분할된 격자는 하위 탐색 목록에 추가 후 넘어감
						log.debug("    - [캐시된 분할 격자] 하위 탐색 목록에 추가 (좌표: {},{})", center[0], center[1]);
						denseSubPolygons.add(GridUtil.createPolygonForCell(center[0], center[1], radius));
						continue;
					}
				}

				// 3-3. (이미 완료하거나 분할 처리한 격자가 아닌 탐색 대상) 신규 격자에 대해 카카오 API 호출하여 밀집도 검사
				log.debug("    - [신규 격자] 밀집도 검사 API 호출 (좌표: {},{})", center[0], center[1]);
				KakaoResponse response = callKakaoApiWithRetry(
					() -> kakaoApiService.searchPlaces(keyword, center[1], center[0], radius, 1), "키워드 검색");
				if (response == null || response.getDocuments() == null)
					continue;

				// 3-4. API 응답에서 총 장소 수 확인
				int totalCount = response.getMeta().getTotal_count();

				// 3-5. 밀집도에 따른 처리 분기
				if (totalCount > DENSE_AREA_THRESHOLD) {
					// 밀집도가 높은 지역: 하위 폴리곤으로 분할하여 재귀 처리
					denseSubPolygons.add(GridUtil.createPolygonForCell(center[0], center[1], radius));
					// Redis 캐시에 분할 격자 상태 저장
					ScannedGrid subdividedGrid = ScannedGrid.builder()
						.regionName(regionName).keyword(keyword).gridCenterLat(center[0]).gridCenterLng(center[1])
						.gridRadius(radius).status(ScannedGrid.GridStatus.SUBDIVIDED).build();
					cacheGrid(subdividedGrid);
					scannedGridRepository.save(subdividedGrid);
				} else {
					// 일반 지역: 페이지네이션을 통해 모든 장소 데이터 수집 및 저장
					int foundCountInCell = savePaginatedPlaces(response, regionName, keyword, polygon, center, radius,
						foundPlaceIds);
					// Redis 캐시에 완료 격자 상태 저장
					ScannedGrid completedGrid = ScannedGrid.builder()
						.regionName(regionName).keyword(keyword).gridCenterLat(center[0]).gridCenterLng(center[1])
						.gridRadius(radius).status(ScannedGrid.GridStatus.COMPLETED).build();
					cacheGrid(completedGrid);
					scannedGridRepository.save(completedGrid);
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

	/**
	 * 최대 재귀 깊이에 도달하여 강제로 수집을 (병렬적으로) 시작합니다.
	 * <p>내부에서 forceCollectAtMaxDepth() 호출</p>
	 * @param regionName 지역명
	 * @param keyword 검색 키워드
	 * @param polygons 검색할 폴리곤 좌표
	 * @param radius 격자 반경 (미터)
	 * @param foundPlaceIds 중복 방지를 위한 발견된 장소 ID 집합
	 */
	private void forceCollectInParallel(String regionName, String keyword, List<List<List<Double>>> polygons,
		int radius, Set<String> foundPlaceIds) {
		List<Callable<Void>> tasks = new ArrayList<>();
		for (List<List<Double>> polygon : polygons) {
			tasks.add(() -> {
				forceCollectAtMaxDepth(regionName, keyword, polygon, radius, foundPlaceIds);
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

	/**
	 * 최대 재귀 깊이에 도달하여 강제로 수집을 시작합니다.
	 * @param regionName 지역명
	 * @param keyword 검색 키워드
	 * @param polygon 검색할 폴리곤 좌표
	 * @param radius 격자 반경 (미터)
	 * @param foundPlaceIds 중복 방지를 위한 발견된 장소 ID 집합
	 */
	private void forceCollectAtMaxDepth(String regionName, String keyword, List<List<Double>> polygon,
		int radius, Set<String> foundPlaceIds) {
		GridUtil.BoundingBox bbox = GridUtil.getBoundingBoxForPolygon(polygon);
		List<double[]> gridCenters = GridUtil.generateGridForBoundingBox(bbox.getLatStart(),
			bbox.getLatEnd(), bbox.getLngStart(), bbox.getLngEnd(), radius);

		for (double[] center : gridCenters) {
			if (GridUtil.isPointNotInPolygon(center[0], center[1], polygon))
				continue;
			savePaginatedPlaces(null, regionName, keyword, polygon, center, radius, foundPlaceIds);
		}
	}

	/**
	 * 페이지네이션 검색 및 장소 저장을 수행합니다.
	 * <p>내부에서 savePlaces() 호출</p>
	 * @param firstPageResponse 첫 페이지 응답
	 * @param regionName 지역명
	 * @param keyword 검색 키워드
	 * @param regionPolygon 지역 폴리곤 좌표
	 * @param center 격자 중심점
	 * @param radius 격자 반경 (미터)
	 * @param foundPlaceIds 중복 방지를 위한 발견된 장소 ID 집합
	 * @return 실제로 저장된 장소 수
	 */
	private int savePaginatedPlaces(KakaoResponse firstPageResponse, String regionName, String keyword,
		List<List<Double>> regionPolygon, double[] center, int radius, Set<String> foundPlaceIds) {
		int foundCount = 0;
		try {
			// 1. 첫 페이지 응답 처리
			KakaoResponse currentResponse = firstPageResponse;
			int currentPage = 1;

			if (currentResponse == null) {
				final int pageForFirstCall = currentPage;
				currentResponse = callKakaoApiWithRetry(
					() -> kakaoApiService.searchPlaces(keyword, center[1], center[0], radius, pageForFirstCall),
					"페이지네이션 검색 (1페이지)");
			}

			// 2. 페이지네이션 검색 및 장소 저장
			while (true) {
				if (currentResponse == null || currentResponse.getDocuments() == null || currentResponse.getDocuments()
					.isEmpty()) {
					break;
				}

				int savedInPage = savePlaces(currentResponse.getDocuments(), regionName, keyword, regionPolygon,
					foundPlaceIds);
				if (savedInPage > 0) {
					log.info("        - 페이지 {}에서 {}개의 새 장소를 DB에 저장 시도.", currentPage, savedInPage);
				}
				foundCount += savedInPage;

				if (currentResponse.getMeta().is_end())
					break;

				currentPage++;
				if (currentPage > MAX_PAGE_PER_QUERY)
					break;

				// 3. 다음 페이지 검색
				final int pageForNextCall = currentPage;
				currentResponse = callKakaoApiWithRetry(
					() -> kakaoApiService.searchPlaces(keyword, center[1], center[0], radius, pageForNextCall),
					"페이지네이션 검색 ({%d}페이지)".formatted(currentPage));
			}
		} catch (Exception e) {
			if (e instanceof InterruptedException)
				Thread.currentThread().interrupt();
			log.error("    - 페이지네이션 수집 중 오류 발생: {}", e.getMessage());
		}
		return foundCount;
	}

	/**
	 * 카카오 API에서 수집한 장소 데이터를 데이터베이스에 저장합니다.
	 *
	 * <p>이 메서드는 데이터 수집 프로세스의 핵심 부분으로, 다음과 같은 과정을 거칩니다:</p>
	 * <ol>
	 *   <li><b>데이터 변환:</b> KakaoPlace DTO를 PlaceEntity로 변환</li>
	 *   <li><b>위치 검증:</b> 장소가 지정된 폴리곤 내부에 있는지 확인</li>
	 *   <li><b>중복 검사:</b> 메모리 Set을 사용한 중복 방지</li>
	 *   <li><b>배치 저장:</b> JPA saveAll()을 통한 효율적인 DB 저장</li>
	 *   <li><b>예외 처리:</b> 중복 데이터 등 무결성 제약 조건 처리</li>
	 * </ol>
	 *
	 * @param places 카카오 API에서 수집한 장소 목록
	 * @param regionName 지역명
	 * @param keyword 검색 키워드
	 * @param regionPolygon 지역 폴리곤 (위치 검증용)
	 * @param foundPlaceIds 중복 방지를 위한 발견된 장소 ID 집합
	 * @return 실제로 저장된 장소 수
	 */
	private int savePlaces(List<KakaoPlace> places, String regionName, String keyword,
		List<List<Double>> regionPolygon, Set<String> foundPlaceIds) {

		List<PlaceEntity> placeEntities = new ArrayList<>();

		// 1. 각 장소에 대해 데이터 변환 및 검증 수행
		for (KakaoPlace place : places) {
			// 1-1. 좌표 파싱 (카카오 API는 문자열로 반환)
			double lat = Double.parseDouble(place.getY());
			double lng = Double.parseDouble(place.getX());

			// 1-2. 폴리곤 내부 위치 검증
			if (GridUtil.isPointNotInPolygon(lat, lng, regionPolygon)) {
				continue; // 폴리곤 외부 장소는 제외
			}

			// 1-3. 메모리 기반 중복 검사 (DB 조회보다 빠름)
			if (foundPlaceIds.contains(place.getId())) {
				continue; // 이미 이번 작업에서 추가된 장소이므로 건너뛰기
			}

			// 1-4. KakaoPlace → PlaceEntity 변환
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
			foundPlaceIds.add(place.getId()); // 중복 방지를 위해 Set에 추가
		}

		// 2. 저장할 데이터가 없으면 0 반환
		if (placeEntities.isEmpty()) {
			return 0;
		}

		// 3. JPA를 통한 배치 저장 (효율적인 DB 작업)
		try {
			collectorPlaceRepository.saveAll(placeEntities);
			return placeEntities.size();
		} catch (DataIntegrityViolationException e) {
			// 4. 중복 데이터 등 무결성 제약 조건 위반 시 로그만 남기고 계속 진행
			log.warn("    - 데이터 저장 중 무결성 제약 조건 위반 발생 (예: 중복 데이터). 건너뜁니다.");
			return 0;
		}
	}

	/**
	 * MAX_RETRIES 횟수만큼 카카오 API 호출을 재시도하고 실패하면 예외를 발생시킵니다.
	 * @param <T> 반환 타입
	 * @param apiCall 카카오 API 호출
	 * @param errorMessage 에러 메시지
	 * @return 반환 값
	 * @throws Exception 예외
	 */
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
			}
			// 기타 Exception들(InterruptedException, TimeoutException 등)은 자동으로 상위로 전파됨
		}
		throw new RuntimeException("API 호출 최대 재시도 횟수 초과: " + errorMessage);
	}

	/**
	 * Redis에서 격자 상태를 조회
	 *
	 * <p>지정된 좌표와 반경에 해당하는 격자의 처리 상태를 Redis 캐시에서 조회합니다.
	 * 캐시에 데이터가 없으면 데이터베이스에서 조회하여 반환합니다.</p>
	 *
	 * <p><strong>캐시 키 형식:</strong> {regionName}_{keyword}_{lat}_{lng}_{radius}</p>
	 *
	 * @param regionName 지역명
	 * @param keyword 검색 키워드
	 * @param lat 격자 중심 위도
	 * @param lng 격자 중심 경도
	 * @param radius 격자 반경 (미터)
	 * @return 캐시된 격자 정보 (없으면 null)
	 */
	@Cacheable(value = "gridCache", key = "#regionName + '_' + #keyword + '_' + #lat + '_' + #lng + '_' + #radius")
	public ScannedGrid getCachedGrid(String regionName, String keyword, double lat, double lng, int radius) {
		// Redis 캐시에서 조회 (실패 시 DB에서 조회)
		Optional<ScannedGrid> existingGrid = scannedGridRepository.findByRegionNameAndKeywordAndGridCenterLatAndGridCenterLngAndGridRadius(
			regionName, keyword, lat, lng, radius);
		return existingGrid.orElse(null);
	}

	/**
	 * Redis에 격자 상태를 저장
	 *
	 * <p>처리된 격자의 상태 정보를 Redis 캐시에 저장합니다.
	 * 이후 동일한 격자에 대한 조회 시 데이터베이스 접근 없이 캐시에서 빠르게 조회할 수 있습니다.</p>
	 *
	 * <p><strong>캐시 키 형식:</strong> {regionName}_{keyword}_{lat}_{lng}_{radius}</p>
	 *
	 * @param grid 저장할 격자 정보
	 * @return 저장된 격자 정보
	 */
	@CachePut(value = "gridCache", key = "#grid.regionName + '_' + #grid.keyword + '_' + #grid.gridCenterLat + '_' + #grid.gridCenterLng + '_' + #grid.gridRadius")
	public ScannedGrid cacheGrid(ScannedGrid grid) {
		return grid;
	}
}
