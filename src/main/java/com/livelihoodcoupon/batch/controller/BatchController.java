package com.livelihoodcoupon.batch.controller;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
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
 *   <li><code>POST /admin/batch/csv-to-db</code> - Place CSV → DB 배치 실행</li>
 *   <li><code>POST /admin/batch/parking-csv-to-db</code> - ParkingLot CSV → DB 배치 실행</li>
 *   <li><code>POST /admin/batch/status</code> - 배치 시스템 상태 확인</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/admin/batch")
@RequiredArgsConstructor
@Profile("!test")
public class BatchController {

	private final JobLauncher jobLauncher;
	private final Job placeCsvJob;
	private final Job parkingLotCsvJob;

	/**
	 * CSV 파일을 데이터베이스로 로드하는 배치 작업을 실행합니다.
	 *
	 * <p>data/csv/ 디렉토리에 있는 모든 CSV 파일을 읽어서 데이터베이스에 저장합니다.
	 * Redis 캐시를 사용하여 중복 데이터를 효율적으로 처리합니다.</p>
	 *
	 * <p><strong>주의사항:</strong> 이 작업은 대용량 데이터를 처리하므로 완료까지 시간이 걸릴 수 있습니다.
	 * 배치 작업은 백그라운드에서 비동기적으로 실행됩니다.</p>
	 *
	 * @return 배치 작업 시작 성공 메시지
	 */
	@PostMapping("/csv-to-db")
	public ResponseEntity<CustomApiResponse<?>> runCsvToDbBatch() {
		try {
			log.info("CSV to DB 배치 작업 시작 요청됨");

			// 배치 작업을 비동기적으로 실행
			new Thread(() -> {
				try {
					JobParameters jobParameters = new JobParametersBuilder()
						.addString("JobID", String.valueOf(System.currentTimeMillis()))
						.toJobParameters();

					jobLauncher.run(placeCsvJob, jobParameters);
					log.info("CSV to DB 배치 작업 완료됨");
				} catch (Exception e) {
					log.error("CSV to DB 배치 작업 실행 중 오류 발생", e);
				}
			}).start();

			return ResponseEntity.ok(
				CustomApiResponse.success("CSV to DB 배치 작업이 백그라운드에서 시작되었습니다.")
			);
		} catch (Exception e) {
			log.error("CSV to DB 배치 작업 시작 중 오류 발생", e);
			return ResponseEntity.internalServerError()
				.body(CustomApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR,
					"배치 작업 시작 중 오류가 발생했습니다: " + e.getMessage()));
		}
	}

	/**
	 * ParkingLot CSV(전국주차장정보표준데이터.csv)를 DB로 적재하는 배치 작업 실행 (비동기)
	 */
	@PostMapping("/parking-csv-to-db")
	public ResponseEntity<CustomApiResponse<?>> runParkingCsvToDbBatch() {
		try {
			log.info("[Batch] ParkingLot CSV to DB 배치 작업 시작 요청됨");

			new Thread(() -> {
				try {
					JobParameters jobParameters = new JobParametersBuilder()
						.addString("JobID", String.valueOf(System.currentTimeMillis()))
						.toJobParameters();

					jobLauncher.run(parkingLotCsvJob, jobParameters);
					log.info("[Batch] ParkingLot CSV to DB 배치 작업 완료됨");
				} catch (Exception e) {
					log.error("[Batch] ParkingLot CSV to DB 배치 작업 실행 중 오류 발생", e);
				}
			}).start();

			return ResponseEntity.ok(
				CustomApiResponse.success("ParkingLot CSV to DB 배치 작업이 백그라운드에서 시작되었습니다.")
			);
		} catch (Exception e) {
			log.error("[Batch] ParkingLot CSV to DB 배치 작업 시작 중 오류 발생", e);
			return ResponseEntity.internalServerError()
				.body(CustomApiResponse.error(
					ErrorCode.INTERNAL_SERVER_ERROR,
					"배치 작업 시작 중 오류가 발생했습니다: " + e.getMessage()
				));
		}
	}

	/**
	 * 배치 시스템 상태를 확인합니다.
	 *
	 * @return 배치 시스템 상태 정보
	 */
	@PostMapping("/status")
	public ResponseEntity<CustomApiResponse<?>> getBatchStatus() {
		try {
			log.info("배치 시스템 상태 확인 요청됨");

			return ResponseEntity.ok(
				CustomApiResponse.success(
					"배치 시스템이 정상적으로 초기화되었습니다. JobLauncher: " + (
						jobLauncher != null ? "OK" : "NULL") + ", Job: " + (
						placeCsvJob != null ? "OK" : "NULL"))
			);
		} catch (Exception e) {
			log.error("배치 시스템 상태 확인 중 오류 발생", e);
			return ResponseEntity.internalServerError()
				.body(CustomApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR,
					"배치 시스템 상태 확인 중 오류가 발생했습니다: " + e.getMessage()));
		}
	}
}
