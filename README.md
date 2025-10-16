# Livelihood Coupon Backend
- [Livelihood Coupon Frontend 바로가기](https://github.com/livelihoodCoupon/Frontend)

> **민생회복쿠폰 지도 서비스의 백엔드** 레포지토리입니다. </br>
> Spring Boot와 Elasticsearch를 기반으로 데이터 수집, 검색, 경로 안내 API를 제공합니다.

</br>

## 1. 프로젝트 개요

이 레포지토리는 전국 소비쿠폰 사용 가능 장소 데이터를 수집, 저장, 검색하는 백엔드 시스템으로, 다음과 같은 핵심 기능을 제공합니다:

- **데이터 수집**: 카카오 API를 통한 전국 소비쿠폰 장소 데이터 수집
- **검색 서비스**: Elasticsearch 기반 고성능 장소 검색 및 자동완성
- **주차장 정보**: 주변 주차장 검색 및 상세 정보 제공
- **경로 안내**: 카카오 API와 OSRM을 활용한 다중 경로 제공
- **배치 처리**: Spring Batch를 활용한 대용량 데이터 처리

</br>

## 2. 기술 스택

### 핵심
- **Java 21** - 최신 LTS 버전
- **Spring Boot 3.5.5** - 메인 프레임워크
- **Spring Data JPA** - 데이터 접근 계층
- **Spring Batch** - 대용량 데이터 배치 처리
- **Spring WebFlux** - 비동기 웹 처리

### 데이터베이스 & 캐시
- **PostgreSQL 16** - 메인 데이터베이스
- **PostGIS** - 공간 데이터 처리
- **Redis 7.4** - 캐싱 및 세션 관리

### 검색 & 분석 & 모니터링 & 로깅
- **Elasticsearch 8.14** - 검색 엔진
- **Logstash** - 로그 처리
- **Kibana** - 데이터 시각화
- **Prometheus** - 메트릭 수집
- **Grafana** - 모니터링 대시보드
- **Micrometer** - 애플리케이션 메트릭

### 외부 API
- **카카오 로컬 API** - 장소 데이터 수집
- **카카오내비 API** - 자동차 경로 안내 (비상 시 OSRM 대체)
- **OSRM** - 도보/자전거/자동차/대중교통 경로

### 인프라
- **Docker & Docker Compose** - 컨테이너화
- **Gradle** - 빌드 도구
- **Lombok** - 코드 간소화

</br>

## 3. 프로젝트 구조

```
livelihoodCoupon/
├── src/main/java/com/livelihoodcoupon/
│   ├── batch/                    # 배치 처리
│   │   ├── controller/           # 배치 관리 API
│   │   ├── config/              # 배치 설정
│   │   └── service/             # 배치 서비스
│   ├── collector/                # 데이터 수집
│   │   ├── controller/          # 수집 관리 API
│   │   ├── entity/             # 수집 엔티티
│   │   ├── service/            # 수집 서비스
│   │   └── vo/                 # 값 객체
│   ├── common/                  # 공통 모듈
│   │   ├── config/             # 공통 설정
│   │   ├── entity/             # 기본 엔티티
│   │   ├── exception/           # 예외 처리
│   │   └── response/            # 응답 형식
│   ├── parkinglot/             # 주차장 관리
│   │   ├── controller/         # 주차장 API
│   │   ├── entity/            # 주차장 엔티티
│   │   ├── repository/        # 주차장 저장소
│   │   └── service/           # 주차장 서비스
│   ├── place/                  # 장소 관리
│   │   ├── controller/          # 장소 API
│   │   ├── entity/           # 장소 엔티티
│   │   ├── repository/       # 장소 저장소
│   │   └── service/          # 장소 서비스
│   ├── route/                 # 경로 안내
│   │   ├── controller/       # 경로 API
│   │   ├── dto/              # 경로 DTO
│   │   ├── service/          # 경로 서비스
│   │   └── util/             # 경로 유틸리티
│   ├── search/               # 검색 서비스
│   │   ├── controller/      # 검색 API
│   │   ├── entity/         # 검색 엔티티
│   │   ├── repository/     # 검색 저장소
│   │   └── service/        # 검색 서비스
│   └── LivelihoodCouponApplication.java
├── src/main/resources/
│   ├── application.yml          # 메인 설정
│   ├── application-dev.yml      # 개발 환경 설정
│   ├── logback-spring.xml       # 로깅 설정
│   └── static/                  # 정적 리소스
├── infra/                       # 인프라 설정
│   ├── database/               # 데이터베이스 설정
│   ├── elk/                    # ELK 스택 설정
│   ├── monitoring/              # 모니터링 설정
│   ├── osrm/                   # OSRM 설정
│   └── scripts/                # 실행 스크립트
├── data/                        # 데이터 파일
│   ├── csv/                    # CSV 데이터
│   ├── geojson/                # GeoJSON 데이터
│   └── parkinglotcsv/          # 주차장 CSV
├── logs/                        # 로그 파일
├── docker-compose.yml           # 메인 Docker Compose
├── Dockerfile                  # 애플리케이션 Dockerfile
└── build.gradle                 # Gradle 빌드 설정
```

</br>

## 4. 주요 기능

### 데이터 수집
- **전국 데이터 수집**: 모든 지역의 소비쿠폰 장소 데이터 자동 수집
- **지역별 수집**: 특정 지역 선택적 데이터 수집
- **격자 기반 수집**: 효율적인 공간 데이터 수집을 위한 격자 시스템
- **중복 제거**: 스마트한 중복 데이터 처리

### 검색 서비스
- **위치 기반 검색**: 사용자 위치를 중심으로 한 반경 검색
- **키워드 검색**: 자연어 처리 기반 스마트 검색
- **자동완성**: 실시간 검색어 제안
- **Elasticsearch 통합**: 고성능 검색 엔진 활용

### 주차장 정보
- **주변 주차장 검색**: 위치 기반 주차장 검색
- **상세 정보 제공**: 요금, 운영시간, 시설 정보
- **거리 계산**: 사용자 위치 기준 거리 정보

### 경로 안내
- **다양한 교통수단**: 자동차, 도보, 자전거, 대중교통
- **하이브리드 시스템**: 카카오 API + OSRM 조합
- **실패 복구**: 주요 제공자 실패 시 대체 제공자 자동 전환

### 배치 처리
- **CSV 데이터 로드**: 대용량 CSV 파일 데이터베이스 적재
- **Elasticsearch 색인**: 검색 엔진 데이터 동기화
- **증분 처리**: 새로운 데이터만 추가 처리

</br>

## 5. API 엔드포인트

### 클라이언트 API (`/api`)

#### 검색 API
- `GET /api/search` - DB 기반 장소 검색 (비권장)
- `GET /api/searches` - Elasticsearch 기반 장소 검색
- `GET /api/searches/{id}` - 장소 상세 정보 조회
- `GET /api/suggestions` - 검색어 자동완성

#### 주차장 API
- `GET /api/searches/parkinglots-es` - 주차장 검색
- `GET /api/parkinglots/{id}` - 주차장 상세 정보 조회

#### 경로 API
- `GET /api/routes/search` - 경로 조회
- `GET /api/routes/providers` - 경로 제공자(카카오/OSRM) 목록

### 관리자 API (`/admin`)

#### 데이터 수집
- `GET /admin/collect/nationwide` - 전국 데이터 수집
- `GET /admin/collect/{regionName}` - 지역별 데이터 수집

#### 배치 관리
- `POST /admin/batch/db/all-csv` - 전체 CSV 데이터 로드
- `POST /admin/batch/db/new-csv` - 증분 CSV 데이터 추가 로드
- `POST /admin/batch/db/parking-csv-to-db` - 주차장 CSV 데이터 로드 (비권장)
- `GET /admin/batch/db/status` - 배치 상태 확인

#### Elasticsearch 관리
- `POST /admin/es/indices` - 인덱스 생성
- `DELETE /admin/es/indices` - 인덱스 삭제
- `POST /admin/batch/es/all-csv` - 전체 데이터 색인
- `POST /admin/batch/es/new-csv` - 증분 데이터 색인
- `POST /admin/batch/es/parkinglots-index` - 주차장 인덱스 생성
- `POST /admin/batch/es/parkinglots-csv` - 주차장 CSV 데이터 색인

#### 시스템 관리
- `GET /admin/health` - 시스템 상태 확인
- `POST /admin/exports/csv` - 데이터 CSV 내보내기
- `POST /admin/geocode/backfill` - 지오코딩 재시도

</br>

## 6. 시작하기

### 요구사항
- Java 21
- Docker & Docker Compose
- Git
- Kakao Maps API KEY

- IntelliJ IDEA 권장
- **Lombok** 플러그인 설치
- **Checkstyle** 플러그인 설치 (Naver 스타일 가이드)

### 1) 저장소 클론
```bash
git clone https://github.com/livelihoodCoupon/Server-Backend.git
cd Server-Backend
```

### 2) 환경 변수 설정
`.env.example` 파일을 복사해 `.env` 파일을 생성하고 필요한 환경 변수를 설정합니다.

### 3) OSRM 데이터 준비 (최초 1회만)
```bash
# OSRM 데이터 다운로드 및 설정 (시간이 매우 오래 걸림)
cd infra/osrm
./setup-osrm.sh
```

### 4) 인프라 서비스 시작 & 애플리케이션 빌드 및 실행
```bash
./infra/scripts/start.sh

# 또는 로컬 실행
./gradlew bootRun
```

### 5) 서비스 중지
```bash
# 모든 서비스 중지
./infra/scripts/stop.sh
```

</br>

## 7. 모니터링 및 로깅

### 로그 확인
- **애플리케이션 로그**: `logs/application.log`
- **Docker 로그**: `docker logs livelihoodCoupon-collector-app-1`
- **Kibana**: http://{}:5601 (로그 시각화)

### 메트릭 모니터링
- **Prometheus**: http://{}:9090 (메트릭 수집)
- **Grafana**: http://{}:3001 (대시보드)
- **Actuator**: http://{}:8080/actuator (애플리케이션 상태)

### 성능 모니터링
- **JVM 메트릭**: 메모리, GC, 스레드 정보
- **HTTP 메트릭**: 요청/응답 시간, 에러율
- **데이터베이스 메트릭**: 연결 풀, 쿼리 성능
- **캐시 메트릭**: Redis 히트율, 응답 시간
