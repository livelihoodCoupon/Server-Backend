package com.livelihoodcoupon.batch.controller;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.MDC;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.livelihoodcoupon.common.exception.ErrorCode;
import com.livelihoodcoupon.common.response.CustomApiResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 배치 작업 관련 API를 제공하는 컨트롤러
 *
 * <h3>주요 기능:</h3>
 * <ul>
 *   <li><b>CSV 배치 실행:</b> CSV 파일을 데이터베이스로 로드하는 배치 작업 실행</li>
 *   <li><b>주차장 CSV 배치 실행:</b> 전국주차장정보표준데이터.csv를 DB에 적재</li>
 *   <li><b>상태 확인:</b> 배치 시스템 초기화 여부 확인</li>
 * </ul>
 *
 * <h3>API 엔드포인트:</h3>
 * <ul>
 *   <li><code>POST /admin/batch/db/all-csv</code> - data/csv 경로의 전체 CSV 파일을 DB로 로드하는 배치 실행</li>
 *   <li><code>POST /admin/batch/db/new-csv</code> - data/new-csv 경로의 증분 CSV 파일을 DB로 로드하는 배치 실행</li>
 *   <li><code>POST /admin/batch/db/parking-csv-to-db</code> - ParkingLot CSV → DB 배치 실행</li>
 *   <li><code>GET /admin/batch/db/status</code> - 배치 시스템 상태 확인</li>
 * </ul>
 */

@Slf4j
@RestController
@RequestMapping("/admin/batch/db")
@RequiredArgsConstructor
@Profile("!test")
public class BatchController {

	private final JobLauncher jobLauncher;
	private final Job placeCsvJob;
	private final Job placeCsvIncrementalAddJob;
	private final Job parkingLotCsvJob;

	// ① 동시 실행 방지 플래그
	private final AtomicBoolean parkingBatchRunning = new AtomicBoolean(false);
	private final AtomicBoolean placeBatchRunning = new AtomicBoolean(false);

	/**
	 * CSV 파일을 데이터베이스로 로드하는 전체 재구성 배치 작업을 실행합니다.
	 *
	 * <p>data/csv/ 디렉토리에 있는 모든 CSV 파일을 읽어서 데이터베이스에 저장합니다.</p>
	 *
	 * <p><strong>주의사항:</strong> 이 작업은 대용량 데이터를 처리하므로 완료까지 시간이 걸릴 수 있습니다.
	 * 배치 작업은 백그라운드에서 비동기적으로 실행됩니다.</p>
	 *
	 * <p>
	 * <strong>주의:</strong> 이 API는 'placeIds' 캐시를 모두 삭제하므로,
	 * 모든 CSV 데이터를 처음부터 다시 로드할 때 사용해야 합니다.
	 * </p>
	 *
	 * @return 배치 작업 시작 성공 메시지
	 */
	@PostMapping("/all-csv")
	@CacheEvict(value = "placeIds", allEntries = true)
	public ResponseEntity<CustomApiResponse<?>> runCsvToDbBatchFullReload() {
		try {
			log.info("CSV to DB 전체 재구성 배치 작업 시작 요청됨");
			startBatchJobAsync(placeCsvJob, "placeCsvJob");
			return ResponseEntity.ok(
				CustomApiResponse.success("CSV to DB 전체 재구성 배치 작업이 백그라운드에서 시작되었습니다.")
			);
		} catch (Exception e) {
			log.error("CSV to DB 전체 재구성 배치 작업 시작 중 오류 발생", e);
			return ResponseEntity.internalServerError()
				.body(CustomApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR,
					"배치 작업 시작 중 오류가 발생했습니다: " + e.getMessage()));
		}
	}

	/**
	 * CSV 파일을 데이터베이스로 로드하는 증분 추가 배치 작업을 실행합니다.
	 * <p>
	 * 기존 데이터베이스 및 캐시 상태를 유지한 채, 새로운 데이터만 추가할 때 사용합니다.
	 * </p>
	 *
	 * @return 배치 작업 시작 성공 메시지
	 */
	@PostMapping("/new-csv")
	public ResponseEntity<CustomApiResponse<?>> runCsvToDbBatchIncremental() {
		try {
			log.info("CSV to DB 증분 추가 배치 작업 시작 요청됨");
			startBatchJobAsync(placeCsvIncrementalAddJob, "placeCsvIncrementalAddJob");
			return ResponseEntity.ok(
				CustomApiResponse.success("CSV to DB 증분 추가 배치 작업이 백그라운드에서 시작되었습니다.")
			);
		} catch (Exception e) {
			log.error("CSV to DB 증분 추가 배치 작업 시작 중 오류 발생", e);
			return ResponseEntity.internalServerError()
				.body(CustomApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR,
					"배치 작업 시작 중 오류가 발생했습니다: " + e.getMessage()));
		}
	}

	/**
	 * 배치 작업을 비동기 스레드에서 실행합니다.
	 */
	private void startBatchJobAsync(Job jobToRun, String jobName) {
		new Thread(() -> {
			try {
				JobParameters jobParameters = new JobParametersBuilder()
					.addString("JobID", String.valueOf(System.currentTimeMillis()))
					.toJobParameters();

				jobLauncher.run(jobToRun, jobParameters);
				log.info("{} 배치 작업 완료됨", jobName);
			} catch (Exception e) {
				log.error("{} 배치 작업 실행 중 오류 발생", jobName, e);
			}
		}).start();
	}

	/**
	 * ParkingLot CSV(전국주차장정보표준데이터.csv)를 데이터베이스로 적재하는 배치 작업을 시작합니다.
	 *
	 * <p>주차장 표준 데이터 CSV를 파싱하여 DB에 저장합니다.
	 * 이 엔드포인트는 비동기로 실행되며, 호출 즉시 배치가 백그라운드에서 진행됩니다.</p>
	 *
	 * <h4>특징</h4>
	 * <ul>
	 *   <li><b>HTTP</b>: POST /admin/batch/parking-csv-to-db</li>
	 *   <li><b>응답</b>: 202 Accepted (비동기 실행 수락), 본문에 execId 포함</li>
	 *   <li><b>중복 실행 차단</b>: 서버 프로세스(JVM) 단위로 AtomicBoolean으로 제어</li>
	 *   <li><b>추적</b>: MDC에 execId를 기록하여 로그 상에서 실행 흐름 추적</li>
	 * </ul>
	 *
	 * <h4>주의사항</h4>
	 * <ul>
	 *   <li>파일 용량 및 DB I/O 상황에 따라 처리 시간이 오래 걸릴 수 있습니다.</li>
	 *   <li>이미 실행 중인 경우 409 Conflict를 반환하며, 실행 완료 후 재호출해야 합니다.</li>
	 *   <li>다중 인스턴스/컨테이너 환경에서는 Redis 락 등 분산 락 사용을 권장합니다.</li>
	 * </ul>
	 *
	 * @return 비동기 배치 시작 결과(메시지 및 execId)를 담은 표준 응답
	 */
	@PostMapping("/parking-csv-to-db")
	public ResponseEntity<CustomApiResponse<?>> runParkingCsvToDbBatch() {
		// ② 중복 실행 차단
		if (!parkingBatchRunning.compareAndSet(false, true)) {
			return ResponseEntity.status(409).body(
				CustomApiResponse.error(ErrorCode.CONFLICT, "이미 실행 중입니다.")
			);
		}

		// ③ 요청 시점의 MDC 복사(추적 ID 전파)
		Map<String, String> mdc = MDC.getCopyOfContextMap();
		String execId = UUID.randomUUID().toString();

		Thread t = new Thread(() -> {
			try {
				if (mdc != null) MDC.setContextMap(mdc);
				MDC.put("execId", execId);

				JobParameters params = new JobParametersBuilder()
					.addString("JobID", String.valueOf(System.currentTimeMillis()))
					.addString("execId", execId)
					.toJobParameters();

				log.info("[배치] 주차장 CSV → DB 적재 시작 (execId={})", execId);
				jobLauncher.run(parkingLotCsvJob, params);
				log.info("[배치] 주차장 CSV → DB 적재 완료 (execId={})", execId);

			} catch (Exception e) {
				log.error("[배치] 주차장 CSV → DB 적재 중 오류 발생 (execId={})", execId, e);
			} finally {
				parkingBatchRunning.set(false);
				MDC.clear();
			}
		}, "batch-parking-" + execId);

		// ④ 데몬 스레드 + 예외 핸들러
		t.setDaemon(true);
		t.setUncaughtExceptionHandler((th, ex) ->
			log.error("[배치] 처리되지 않은 예외 발생 (스레드={}, execId={})", th.getName(), execId, ex)
		);
		t.start();

		// ⑤ 수락 응답(코드 유지해도 되지만 202가 더 표준)
		return ResponseEntity.accepted().body(
			CustomApiResponse.success("배치 작업이 시작되었습니다. execId=" + execId)
		);
	}

	/**
	 * 배치 시스템의 간단한 상태를 조회합니다.
	 *
	 * <p>현재 서버 프로세스(JVM)에서 장소/주차장 배치의 실행 여부를 반환합니다.
	 * 실행 중 여부만을 알려주는 경량 상태 체크로, 상세 진행률/히스토리는 별도 저장소가 필요합니다.</p>
	 *
	 * <h4>특징</h4>
	 * <ul>
	 *   <li><b>HTTP</b>: GET /api/batch/status</li>
	 *   <li><b>응답</b>: 200 OK, { parkingRunning: boolean, placeRunning: boolean }</li>
	 *   <li><b>범위</b>: 현재 인스턴스(JVM) 기준 상태 플래그</li>
	 * </ul>
	 *
	 * <h4>주의사항</h4>
	 * <ul>
	 *   <li>다중 인스턴스 환경에서는 인스턴스별 상태가 다를 수 있습니다.</li>
	 *   <li>운영에서 통합 상태를 보려면 실행 로그/상태를 DB/Redis에 기록하고
	 *       별도 조회 API를 구성하는 것을 권장합니다.</li>
	 * </ul>
	 *
	 * @return 현재 인스턴스 기준 배치 실행 상태를 담은 표준 응답
	 */
	// 상태 조회는 GET 권장
	@GetMapping("/status")
	public ResponseEntity<CustomApiResponse<?>> getBatchStatus() {
		var dto = Map.of(
			"parkingRunning", parkingBatchRunning.get(),
			"placeRunning", placeBatchRunning.get()
		);
		log.info("[배치] 상태 조회 요청 - parkingRunning={}, placeRunning={}",
			dto.get("parkingRunning"), dto.get("placeRunning"));
		return ResponseEntity.ok(CustomApiResponse.success(dto));
	}
}
