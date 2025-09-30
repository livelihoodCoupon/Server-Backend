package com.livelihoodcoupon.batch;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile("!test") // test 프로필에서는 실행되지 않도록 설정
public class JobRunner implements CommandLineRunner {

	private final JobLauncher jobLauncher;
	private final Job placeCsvJob;

	@Override
	public void run(String... args) throws Exception {
		// 자동 배치 실행 제거 - 관리자가 필요할 때만 API를 통해 실행
		log.info("JobRunner 초기화 완료. CSV 배치는 /admin/batch/csv-to-db API를 통해 수동 실행하세요.");
		log.info("또한 주차장 CSV 배치는 /admin/batch/parking-csv-to-db API를 통해 수동 실행하세요.");

		// 향후 다른 초기화 작업이 필요하면 여기에 추가
		// 예: 데이터베이스 연결 확인, 캐시 초기화 등
	}
}
