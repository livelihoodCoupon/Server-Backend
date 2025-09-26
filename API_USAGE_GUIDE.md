# API Usage Guide

> 소비쿠폰 데이터 수집 및 관리 시스템의 API 사용 가이드입니다.

</br>

## 시작하기

### 기본 정보
- **Base URL**: `http://localhost:8080`
- **Content-Type**: `application/json`
- **인증**: 현재 인증 없음 (향후 JWT 토큰 기반 인증 예정)

### 응답 형식
모든 API는 일관된 응답 형식을 사용합니다:

```json
{
  "success": true,
  "data": "응답 데이터",
  "timestamp": "2025-09-22T12:00:00.000Z"
}
```

에러 발생 시:
```json
{
  "success": false,
  "error": {
    "code": "ERROR_CODE",
    "message": "에러 메시지"
  },
  "timestamp": "2025-09-22T12:00:00.000Z"
}
```

</br>

## 서버 내부 API (`/admin`)

### 1. 전국 데이터 수집 API
전국 모든 지역의 소비쿠폰 장소 데이터를 수집합니다.

```http
GET /admin/collect/nationwide
```

**응답 예시:**
```json
{
  "success": true,
  "data": "Nationwide data collection started successfully.",
  "timestamp": "2025-09-22T12:00:00.000Z"
}
```

**주의사항:**
- 이 작업은 비동기적으로 실행되며 완료까지 상당한 시간이 소요됩니다
- 대용량 데이터 처리로 인해 서버 리소스 사용량이 높을 수 있습니다

</br>

### 2. 특정 지역 데이터 수집
지정된 지역의 소비쿠폰 장소 데이터를 수집합니다.

```http
GET /admin/collect/{regionName}
```

**Path Parameters:**

- `regionName` (string): 수집할 지역명
    - 예: `서울특별시 종로구`, `경상남도 창원시 의창구`

**응답 예시:**

```json
{
  "success": true,
  "data": "Data collection for '서울특별시 종로구' started successfully.",
  "timestamp": "2025-09-22T12:00:00.000Z"
}
```

**에러 응답:**
```json
{
  "success": false,
  "error": {
    "code": "NOT_FOUND",
    "message": "Region '존재하지않는지역' not found."
  },
  "timestamp": "2025-09-22T12:00:00.000Z"
}
```

</br>

### 3. CSV로 데이터 내보내기 API
수집된 모든 지역 데이터를 CSV 파일로 내보냅니다.

```http
POST /admin/exports/csv
```

**응답 예시:**
```json
{
  "success": true,
  "data": "DB 데이터를 CSV로 내보내는 작업이 백그라운드에서 시작되었습니다.",
  "timestamp": "2025-09-22T12:00:00.000Z"
}
```

**주의사항:**
- 이 작업은 백그라운드에서 비동기적으로 실행됩니다
- 데이터 양에 따라 처리 시간이 오래 걸릴 수 있습니다
- 생성된 파일은 `data/csv/` 디렉토리에 저장됩니다

</br>

### 4. 시스템 상태 체크 (Health Check) API
애플리케이션의 상태를 확인합니다.

```http
GET /admin/health
```

**응답 예시:**
```json
{
  "success": true,
  "data": "애플리케이션이 정상적으로 실행 중입니다.",
  "timestamp": "2025-09-22T12:00:00.000Z"
}
```

</br>

---

## 클라이언트 API (`/api`)

### 1. 장소 검색 API

키워드와 위치 기반으로 소비쿠폰 사용 가능 장소를 검색합니다.

```http
GET /api/search
```

**Query Parameters:**

- `keyword` (string): 검색 키워드 (예: "마트", "편의점", "카페")
- `x` (double): 중심점 경도 (X 좌표)
- `y` (double): 중심점 위도 (Y 좌표)
- `radius` (integer, optional): 검색 반경 (미터 단위, 기본값: 2000)
- `page` (integer, optional): 페이지 번호 (기본값: 1)
- `size` (integer, optional): 페이지 크기 (기본값: 10, 최대: 100)

**응답 예시:**

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "placeId": "1234567890",
        "placeName": "롯데마트 종로점",
        "categoryName": "대형마트",
        "addressName": "서울특별시 종로구 청계천로 400",
        "roadAddressName": "서울특별시 종로구 청계천로 400",
        "phone": "02-1234-5678",
        "x": "126.978652",
        "y": "37.566826",
        "placeUrl": "http://place.map.kakao.com/1234567890"
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 10,
      "sort": {
        "sorted": false,
        "unsorted": true
      }
    },
    "totalElements": 1,
    "totalPages": 1,
    "first": true,
    "last": true,
    "numberOfElements": 1
  },
  "timestamp": "2025-09-22T12:00:00.000Z"
}
```

**에러 응답:**

```json
{
  "success": false,
  "error": {
    "code": "C002",
    "message": "검색 키워드가 필요합니다."
  },
  "timestamp": "2025-09-22T12:00:00.000Z"
}
```

</br>

### 2. 장소 상세 정보 조회 API

특정 장소의 상세 정보를 조회합니다.

```http
GET /api/places/{placeId}
```

**Path Parameters:**

- `placeId` (string): 장소 ID

**응답 예시:**

```json
{
  "success": true,
  "data": {
    "placeId": "1234567890",
    "placeName": "롯데마트 종로점",
    "categoryName": "대형마트",
    "addressName": "서울특별시 종로구 청계천로 400",
    "roadAddressName": "서울특별시 종로구 청계천로 400",
    "phone": "02-1234-5678",
    "x": "126.978652",
    "y": "37.566826",
    "placeUrl": "http://place.map.kakao.com/1234567890"
  },
  "timestamp": "2025-09-22T12:00:00.000Z"
}
```

</br>

### 3. 길찾기 API

출발지와 도착지 사이의 경로를 조회합니다. 하이브리드 시스템으로 카카오 API(자동차)와 OSRM(도보/자전거/대중교통)을 지원합니다.

```http
GET /api/routes/search
```

**Query Parameters:**

- `startLng` (double): 출발지 경도 (X 좌표)
- `startLat` (double): 출발지 위도 (Y 좌표)
- `endLng` (double): 도착지 경도 (X 좌표)
- `endLat` (double): 도착지 위도 (Y 좌표)
- `routeType` (string): 경로 타입
    - `driving`: 자동차 경로 (카카오 API 사용)
    - `walking`: 도보 경로 (OSRM 사용)
    - `cycling`: 자전거 경로 (OSRM 사용)
    - `transit`: 대중교통 경로 (OSRM 사용)

**응답 예시 (자동차 경로):**

```json
{
  "coordinates": [
    {
      "lng": 127.02759059417166,
      "lat": 37.497949935830974
    },
    {
      "lng": 127.02766823190767,
      "lat": 37.49806771206318
    }
  ],
  "totalDistance": 420.0,
  "totalDuration": 148.0,
  "routeType": "DRIVING",
  "steps": [
    {
      "instruction": "출발지",
      "distance": 0.0,
      "duration": 0.0,
      "startLocation": {
        "lng": 127.02759059417166,
        "lat": 37.497949935830974
      },
      "endLocation": {
        "lng": 127.02759059417166,
        "lat": 37.497949935830974
      }
    },
    {
      "instruction": "우회전",
      "distance": 190.0,
      "duration": 57.0,
      "startLocation": {
        "lng": 127.02697034078221,
        "lat": 37.49955771701745
      },
      "endLocation": {
        "lng": 127.02697034078221,
        "lat": 37.49955771701745
      }
    }
  ]
}
```

**응답 예시 (도보/자전거 경로):**

```json
{
  "coordinates": [
    {
      "lng": 127.027641,
      "lat": 37.497897
    },
    {
      "lng": 127.02863,
      "lat": 37.498918
    }
  ],
  "totalDistance": 429.1,
  "totalDuration": 61.9,
  "routeType": "WALKING",
  "steps": [
    {
      "instruction": null,
      "distance": 8.4,
      "duration": 5.5,
      "startLocation": {
        "lng": 127.027641,
        "lat": 37.497897
      },
      "endLocation": {
        "lng": 127.027641,
        "lat": 37.497897
      }
    }
  ]
}
```

**에러 응답:**

```json
{
  "success": false,
  "error": {
    "code": "UNSUPPORTED_ROUTE_TYPE",
    "message": "지원하지 않는 경로 타입입니다: INVALID_TYPE"
  },
  "timestamp": "2025-09-22T12:00:00.000Z"
}
```

</br>

### 4. 경로 제공자 목록 조회 API

사용 가능한 경로 제공자 목록을 조회합니다.

```http
GET /api/routes/providers
```

**응답 예시:**

```json
[
  "KakaoNavi",
  "OSRM"
]
```

**설명:**

- `KakaoNavi`: 카카오내비 API를 사용하여 자동차 경로 제공
- `OSRM`: Open Source Routing Machine을 사용하여 도보/자전거/대중교통 경로 제공

</br>

### 5. 길찾기 모니터링 API

길찾기 API의 성능 및 통계 정보를 조회합니다.

#### 5.1. API 호출 통계 조회

```http
GET /admin/routes/monitoring/stats
```

**응답 예시:**

```json
{
  "success": true,
  "data": {
    "kakao": {
      "totalCalls": 150,
      "successCount": 145,
      "failureCount": 5,
      "successRate": 96.7
    },
    "osrm": {
      "totalCalls": 200,
      "successCount": 195,
      "failureCount": 5,
      "successRate": 97.5
    },
    "fallbackUsage": 3,
    "availableProviders": [
      "KakaoNavi",
      "OSRM"
    ]
  },
  "timestamp": "2025-09-22T12:00:00.000Z"
}
```

#### 5.2. 제공자 헬스체크

```http
GET /admin/routes/monitoring/health
```

**응답 예시:**

```json
{
  "success": true,
  "data": {
    "kakao": {
      "status": "UP",
      "message": "카카오 API 정상"
    },
    "osrm": {
      "status": "UP",
      "message": "OSRM 서버 정상"
    },
    "overall": "UP"
  },
  "timestamp": "2025-09-22T12:00:00.000Z"
}
```

#### 5.3. 성능 메트릭 조회

```http
GET /admin/routes/monitoring/performance
```

**응답 예시:**

```json
{
  "success": true,
  "data": {
    "kakao": {
      "mean": 520.5,
      "max": 1200.0,
      "count": 150
    },
    "osrm": {
      "mean": 25.3,
      "max": 100.0,
      "count": 200
    },
    "driving": {
      "mean": 300.2,
      "max": 800.0,
      "count": 100
    },
    "walking": {
      "mean": 15.8,
      "max": 50.0,
      "count": 80
    }
  },
  "timestamp": "2025-09-22T12:00:00.000Z"
}
```

</br>

## 에러 코드

### HTTP 상태 코드별 에러

| HTTP 상태 | 에러 코드  | 설명           | 해결 방법               |
|---------|--------|--------------|---------------------|
| `400`   | `C001` | 잘못된 요청       | 요청 파라미터 형식 확인       |
| `400`   | `C002` | 잘못된 입력 값     | 입력 데이터 유효성 검사       |
| `400`   | `C003` | 잘못된 요청 파라미터  | 필수 파라미터 누락 여부 확인    |
| `401`   | `C004` | 인증 실패        | API 키 또는 인증 정보 확인   |
| `403`   | `C005` | 접근 거부        | 권한 확인 또는 관리자 문의     |
| `404`   | `C006` | URL을 찾을 수 없음 | 요청 URL 경로 확인        |
| `404`   | `C007` | 리소스를 찾을 수 없음 | 올바른 지역명 또는 장소 ID 확인 |
| `429`   | `C008` | 요청 한도 초과     | API 호출 제한 확인 후 재시도  |
| `500`   | `C009` | 서버 내부 오류     | 서버 로그 확인 또는 관리자 문의  |

### 카카오 API 관련 에러

| 에러 유형             | 설명               | 해결 방법                     |
|-------------------|------------------|---------------------------|
| `KAKAO_API_ERROR` | 카카오 API 호출 실패    | API 키 확인 또는 카카오 API 상태 확인 |
| `API_RATE_LIMIT`  | 카카오 API 호출 제한 초과 | 호출 간격 조정 (현재 30ms 지연 적용)  |

### OSRM API 관련 에러

| 에러 유형                    | 설명            | 해결 방법                                             |
|--------------------------|---------------|---------------------------------------------------|
| `OSRM_API_ERROR`         | OSRM 서버 호출 실패 | OSRM 서버 상태 확인 또는 네트워크 연결 확인                       |
| `UNSUPPORTED_ROUTE_TYPE` | 지원하지 않는 경로 타입 | 올바른 경로 타입 사용 (DRIVING, WALKING, CYCLING, TRANSIT) |

### 길찾기 API 관련 에러

| 에러 코드  | 설명               | 해결 방법                                             |
|--------|------------------|---------------------------------------------------|
| `R001` | 지원하지 않는 경로 타입    | 올바른 경로 타입 사용 (driving, walking, cycling, transit) |
| `R002` | 경로 제공자 서비스 일시 중단 | 잠시 후 재시도 또는 관리자 문의                                |
| `R003` | 경로를 찾을 수 없음      | 출발지/도착지 좌표 확인 또는 다른 경로 타입 시도                      |
| `R004` | 카카오 API 서비스 오류   | 카카오 API 상태 확인 또는 관리자 문의                           |
| `R005` | OSRM 서비스 오류      | OSRM 서버 상태 확인 또는 관리자 문의                           |

</br>

## 주의사항

### API 호출 제한

#### 카카오 API 제한
- **일일 호출 제한**: 300,000회 (카카오 로컬 API 기준)
- **호출 간격**: 30ms (API_CALL_DELAY_MS 설정)
- **재시도 횟수**: 최대 5회 (429 에러 발생 시)
- **페이지당 결과 수**: 15개 (카카오 API 최대값)

#### 시스템 제한
- **동시 요청 제한**: 10개 (서버 리소스에 따라 조정 가능)
- **최대 재귀 깊이**: 9단계 (MAX_RECURSION_DEPTH 설정)
- **최대 페이지 수**: 45페이지 (MAX_PAGE_PER_QUERY 설정)
- **밀집도 임계값**: 15개 (DENSE_AREA_THRESHOLD 설정)

### 데이터 수집 시 고려사항
1. **대용량 처리**: 전국 데이터 수집 시 수시간 소요 가능
2. **메모리 사용량**: 대용량 데이터 처리 시 메모리 사용량 증가
3. **디스크 공간**: CSV/GeoJSON 파일 생성 시 충분한 디스크 공간 필요

### 모니터링
- **로그 확인**: `logs/application.log`에서 상세 로그 확인
- **Docker 로그**: `docker logs livelihoodCoupon-collector-app-1`
- **시스템 리소스**: CPU, 메모리, 디스크 사용량 모니터링

## 관련 링크

- [카카오 로컬 API 문서](https://developers.kakao.com/docs/latest/ko/local/dev-guide)
- [카카오내비 API 문서](https://developers.kakao.com/docs/latest/ko/local/dev-guide#search-directions)
- [OSRM 공식 문서](https://project-osrm.org/docs/v5.24.0/api/)
- [Spring Boot 공식 문서](https://spring.io/projects/spring-boot)
- [PostGIS 공식 문서](https://postgis.net/documentation/)

---

**마지막 업데이트**: 2025-09-25  
**문서 버전**: 1.1.0

