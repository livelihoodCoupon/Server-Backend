package com.livelihoodcoupon.batch;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
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
	private final Job csvToDatabaseJob;

	@Override
	public void run(String... args) throws Exception {
		log.info("Starting the csvToDatabaseJob");
		JobParameters jobParameters = new JobParametersBuilder()
			.addString("JobID", String.valueOf(System.currentTimeMillis()))
			.toJobParameters();
		jobLauncher.run(csvToDatabaseJob, jobParameters);
	}
}
