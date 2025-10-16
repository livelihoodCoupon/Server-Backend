package com.livelihoodcoupon.collector.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.livelihoodcoupon.collector.service.CsvExportService;
import com.livelihoodcoupon.common.response.CustomApiResponse;

import lombok.RequiredArgsConstructor;

/**
 * 데이터 내보내기 관련 API를 제공하는 컨트롤러
 * 
 * <h3>주요 기능:</h3>
 * <ul>
 *   <li><b>CSV 내보내기:</b> 수집된 소비쿠폰 장소 데이터를 CSV 파일로 내보내기</li>
 *   <li><b>비동기 처리:</b> 대용량 데이터 처리를 위한 백그라운드 작업 지원</li>
 * </ul>
 * 
 * <h3>API 엔드포인트:</h3>
 * <ul>
 *   <li><code>POST /admin/exports/csv</code> - 모든 지역 데이터를 CSV로 내보내기</li>
 * </ul>

 */
@RestController
@RequestMapping("/admin/exports")
@RequiredArgsConstructor
public class CsvExportController {

	/** CSV 내보내기 서비스 */
	private final CsvExportService csvExportService;

	/**
	 * CSV 내보내기 작업 시작
	 * 
	 * <p>데이터베이스에 저장된 모든 지역의 소비쿠폰 장소 데이터를 CSV 파일로 내보냅니다.
	 * 이 작업은 백그라운드에서 비동기적으로 실행되므로 즉시 응답을 반환합니다.</p>
	 * 
	 * <p><strong>주의사항:</strong> 데이터 양에 따라 처리 시간이 오래 걸릴 수 있습니다.
	 * 실제 운영 환경에서는 메시지 큐나 별도의 배치 작업을 사용하는 것을 권장합니다.</p>
	 * 
	 * @return 내보내기 작업 시작 성공 메시지
	 */
	@PostMapping("/csv")
	public ResponseEntity<CustomApiResponse<?>> triggerCsvExport() {
		// 실제 운영 환경에서는 데이터 양에 따라 시간이 오래 걸릴 수 있으므로
		// 비동기 처리(별도 스레드나 메시지 큐 사용)를 고려하는 것이 좋습니다.
		new Thread(csvExportService::exportAllRegionsToCsv).start();

		return ResponseEntity.ok(CustomApiResponse.success("DB 데이터를 CSV로 내보내는 작업이 백그라운드에서 시작되었습니다."));
	}

	/**
	 * 주차장 데이터를 CSV로 내보내는 작업을 시작합니다.
	 * @return 내보내기 작업 시작 성공 메시지
	 */
	@PostMapping("/parking-lot-csv")
	public ResponseEntity<CustomApiResponse<?>> triggerParkingLotCsvExport() {
		// 비동기 처리를 위해 새 스레드에서 실행
		new Thread(csvExportService::exportParkingFinalToCsv).start();

		return ResponseEntity.ok(CustomApiResponse.success("주차장 DB 데이터를 CSV로 내보내는 작업이 백그라운드에서 시작되었습니다."));
	}

}
