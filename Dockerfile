# 1. 빌드 단계: Gradle(JDK 21)을 사용하여 프로젝트를 빌드합니다.
FROM gradle:8.8.0-jdk21-alpine AS build
WORKDIR /app

# Gradle 빌드에 필요한 파일들을 먼저 복사하여 Docker 캐시를 활용합니다.
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./

# 전체 소스 코드를 복사합니다.
COPY src ./src

# Gradle 빌드를 실행하여 실행 가능한 JAR 파일을 생성합니다. (테스트는 생략)
RUN ./gradlew build -x test --no-daemon

# 2. 실행 단계: 빌드된 JAR 파일을 가벼운 Java 21 환경에서 실행합니다.
FROM openjdk:21-slim
WORKDIR /app

# 빌드 단계에서 생성된 JAR 파일을 복사합니다.
COPY --from=build /app/build/libs/*.jar app.jar

# 8080 포트를 외부에 노출합니다.
EXPOSE 8080

# 컨테이너가 시작될 때 JAR 파일을 실행하는 명령어를 지정합니다.
ENTRYPOINT ["java", "-jar", "app.jar"]