package com.livelihoodcoupon.batch;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile("!test") // test 프로필에서는 실행되지 않도록 설정
public class JobRunner implements CommandLineRunner {

    private final JobLauncher jobLauncher;
    // Spring 컨테이너의 모든 Job 타입 Bean을 Map 형태로 주입받습니다. (Key: Bean 이름, Value: Job 객체)
    private final Map<String, Job> jobs;

    // application.yml(or properties)의 'job.name' 값을 주입받습니다.
    // 만약 값이 없으면 기본값으로 "NONE"을 사용합니다.
    @Value("${job.name:NONE}")
    private String jobName;

    @Override
    public void run(String... args) throws Exception {
        // job.name이 지정되지 않았거나, "NONE"일 경우 아무것도 실행하지 않고 종료합니다.
        if (jobName.equals("NONE")) {
            log.info("No specific job name configured to run.");
            return;
        }

        // 주입받은 jobs 맵에서 실행할 Job을 찾습니다.
        Job jobToRun = jobs.get(jobName);

        if (jobToRun == null) {
            log.error("Could not find job with name: {}", jobName);
            return;
        }

        log.info("Starting the job: {}", jobName);
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("JobID", String.valueOf(System.currentTimeMillis()))
                .toJobParameters();
        jobLauncher.run(jobToRun, jobParameters);
    }
}