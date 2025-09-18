package com.livelihoodcoupon.collector.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.livelihoodcoupon.collector.service.CsvExportService;
import com.livelihoodcoupon.common.response.CustomApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin/exports")
@RequiredArgsConstructor
public class CsvExportController {

	private final CsvExportService csvExportService;

	@PostMapping("/csv")
	public ResponseEntity<CustomApiResponse<?>> triggerCsvExport() {
		// 실제 운영 환경에서는 데이터 양에 따라 시간이 오래 걸릴 수 있으므로
		// 비동기 처리(별도 스레드나 메시지 큐 사용)를 고려하는 것이 좋습니다.
		new Thread(csvExportService::exportAllRegionsToCsv).start();

		return ResponseEntity.ok(CustomApiResponse.success("DB 데이터를 CSV로 내보내는 작업이 백그라운드에서 시작되었습니다."));
	}
}
