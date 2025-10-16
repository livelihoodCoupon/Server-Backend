package com.livelihoodcoupon.collector.service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 지오코딩 시도 결과를 월별 CSV(events-YYYYMM.csv)에 append-only로 기록.
 * 문자열은 CSV-safe하게 큰따옴표로 감싸고 내부 따옴표는 "" 로 이스케이프.
 */
@Service
public class GeocodeCsv {

	private static final Logger log = LoggerFactory.getLogger(GeocodeCsv.class);

	@Value("${geocode.csv.dir:data/geocode}")
	private String dir;

	private File currentFile() {
		String ym = OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
		new File(dir).mkdirs();
		return new File(dir, "events-" + ym + ".csv");
	}

	private void ensureHeader(File f) throws Exception {
		if (!f.exists()) {
			try (PrintWriter w = new PrintWriter(
				new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8))) {
				w.println("event_id,parking_lot_id,address,lat,lng,confidence,source,status,error,requested_at,resolved_at");
			}
		}
	}

	/** 지오코딩 성공 */
	public void ok(long id, String address, double lat, double lng, double confidence, String source) {
		write(id, address, d(lat), d(lng), s(confidence), source, "OK", null);
	}

	/** 무매칭 */
	public void noMatch(long id, String address, String reason) {
		write(id, address, "", "", "0.0", "kakao", "NO_MATCH", reason);
	}

	/** 예외 */
	public void error(long id, String address, String reason) {
		write(id, address, "", "", "0.0", "kakao", "ERROR", reason);
	}

	/** 공통 쓰기 */
	private void write(long id, String address, String lat, String lng, String confidence,
		String source, String status, String error) {
		File f = currentFile();
		try {
			ensureHeader(f);
			try (PrintWriter w = new PrintWriter(
				new BufferedWriter(new FileWriter(f, true)))) {

				String now = OffsetDateTime.now().toString();

				// 모두 %s로 출력하고, 문자열은 q(), 숫자는 이미 문자열로 전달
				w.printf("%s,%d,%s,%s,%s,%s,%s,%s,%s,%s,%s%n",
					q(UUID.randomUUID().toString()),
					id,
					q(address),
					lat,               // "" 또는 "37.123456"
					lng,               // "" 또는 "127.123456"
					confidence,        // "0.95" 등
					q(source),
					q(status),
					q(error),
					q(now),            // requested_at
					q(now)             // resolved_at
				);
			}
		} catch (Exception e) {
			log.error("CSV write failed: {}", e.getMessage(), e);
		}
	}

	/** 문자열을 CSV-safe 하게 큰따옴표로 감싸고 내부 따옴표 이스케이프 */
	private static String q(String s) {
		if (s == null) return "\"\"";
		return "\"" + s.replace("\"", "\"\"") + "\"";
	}

	/** double 포맷 → 소수 6자리 문자열. 필요 없으면 빈 문자열 반환 */
	private static String d(double v) {
		return String.format(java.util.Locale.ROOT, "%.6f", v);
	}

	/** 소수 포맷 문자열 */
	private static String s(double v) {
		return String.format(java.util.Locale.ROOT, "%.2f", v);
	}
}
