package com.livelihoodcoupon.common.test;

import java.sql.DriverManager;
import java.sql.SQLException;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.containers.wait.strategy.Wait;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
@Testcontainers
public abstract class BaseIntegrationTest {

	// ============================================
	// PostGIS 컨테이너 (멀티 OS / 아키텍처 호환)
	// ============================================
	@Container
	static GenericContainer<?> postgresContainer =
		new GenericContainer<>(DockerImageName.parse("postgis/postgis:16-3.4-alpine"))
			.withExposedPorts(5432)
			.withEnv("POSTGRES_DB", "testdb")
			.withEnv("POSTGRES_USER", "testuser")
			.withEnv("POSTGRES_PASSWORD", "testpass")
			// 맥 M1/M2와 x86_64 윈도우/리눅스 호환
			.withCreateContainerCmdModifier(cmd -> cmd.withPlatform("linux/amd64"))
			.waitingFor(Wait.forListeningPort());

	// ============================================
	// Redis 컨테이너
	// ============================================
	@Container
	static GenericContainer<?> redisContainer =
		new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
			.withExposedPorts(6379)
			.waitingFor(Wait.forListeningPort());

	// ============================================
	// PostGIS 확장 생성 (테스트 시작 전에 설치)
	// ============================================
	@BeforeAll
	static void setupPostGIS() throws SQLException {
		String jdbcUrl = String.format(
			"jdbc:postgresql://%s:%d/testdb",
			postgresContainer.getHost(),
			postgresContainer.getMappedPort(5432)
		);

		System.out.println(">>> PostgreSQL JDBC 연결 시도: " + jdbcUrl);

		try (var connection = DriverManager.getConnection(jdbcUrl, "testuser", "testpass")) {
			try (var statement = connection.createStatement()) {
				statement.execute("CREATE EXTENSION IF NOT EXISTS postgis;");
			}
		}

		System.out.println(">>> PostGIS 확장 설치 완료 (SRID 4326 포함 테스트 가능)");
	}

	// ============================================
	// Spring Boot 속성 등록 (테스트용 DB / Redis)
	// ============================================
	@DynamicPropertySource
	static void registerProperties(DynamicPropertyRegistry registry) {

		// PostgreSQL
		registry.add("spring.datasource.url",
			() -> String.format(
				"jdbc:postgresql://%s:%d/testdb",
				postgresContainer.getHost(),
				postgresContainer.getMappedPort(5432)
			)
		);
		registry.add("spring.datasource.username", () -> "testuser");
		registry.add("spring.datasource.password", () -> "testpass");

		System.out.println(">>> Spring Boot 데이터소스 프로퍼티 등록 완료");

		// Redis
		registry.add("spring.data.redis.host", redisContainer::getHost);
		registry.add("spring.data.redis.port", () -> redisContainer.getFirstMappedPort());
		registry.add("spring.data.redis.ssl.enabled", () -> "false");

		System.out.println(">>> Spring Boot Redis 프로퍼티 등록 완료");

		// 컨테이너 접속 로그
		System.out.println(">>> PostgreSQL 컨테이너 주소: " +
			postgresContainer.getHost() + ":" + postgresContainer.getMappedPort(5432));
		System.out.println(">>> Redis 컨테이너 주소: " +
			redisContainer.getHost() + ":" + redisContainer.getFirstMappedPort());
	}
}
