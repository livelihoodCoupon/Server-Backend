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

### 3. 데이터 내보내기 API (`/admin/exports`)

#### 3.1. 장소 데이터 CSV 내보내기

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

#### 3.2. 주차장 데이터 CSV 내보내기

데이터베이스의 주차장 정보를 CSV 파일로 내보냅니다.

```http
POST /admin/exports/parking-lot-csv
```

**응답 예시:**

```json
{
  "success": true,
  "data": "주차장 DB 데이터를 CSV로 내보내는 작업이 백그라운드에서 시작되었습니다.",
  "timestamp": "2025-10-15T12:00:00.000Z"
}
```

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

### 5. 데이터베이스 배치 관리 API (`/admin/batch/db`)

CSV 파일의 데이터를 데이터베이스로 로드하는 배치 작업을 관리합니다.

#### 5.1. 전체 CSV 데이터 로드

`data/csv/` 경로의 모든 CSV 파일을 읽어 데이터베이스에 전체 데이터를 새로고침합니다.

```http
POST /admin/batch/db/all-csv
```

**응답 예시:**

```json
{
  "success": true,
  "data": "CSV to DB 전체 재구성 배치 작업이 백그라운드에서 시작되었습니다.",
  "timestamp": "2025-10-09T12:00:00.000Z"
}
```

**주의사항:**

- 이 작업은 기존 `place` 테이블의 데이터를 대체하지 않고, 중복되지 않는 데이터만 추가합니다. (내결함성 기능으로 중복 시 건너뜀)
- 작업 전에 `placeIds` 캐시가 삭제됩니다.

</br>

#### 5.2. 증분 CSV 데이터 추가

`data/new-csv/` 경로의 신규 CSV 파일을 읽어 데이터베이스에 증분 추가합니다.

```http
POST /admin/batch/db/new-csv
```

**응답 예시:**

```json
{
  "success": true,
  "data": "CSV to DB 증분 추가 배치 작업이 백그라운드에서 시작되었습니다.",
  "timestamp": "2025-10-09T12:00:00.000Z"
}
```

**주의사항:**

- 이 작업은 기존 데이터를 유지한 채 새로운 데이터만 추가할 때 사용됩니다.

</br>

#### 5.3. 주차장 CSV 데이터 로드

`전국주차장정보표준데이터.csv` 파일을 읽어 데이터베이스에 주차장 정보를 저장(적재)합니다.

```http
POST /admin/batch/db/parking-csv-to-db
```

**응답 예시:**

```json
{
  "success": true,
  "data": "배치 작업이 시작되었습니다. execId=a1b2c3d4-e5f6-g7h8-i9j0-k1l2m3n4o5p6",
  "timestamp": "2025-10-15T12:00:00.000Z"
}
```

**주의사항:**

- 이 작업은 비동기적으로 실행되며, 이미 실행 중인 경우 `409 Conflict` 에러가 발생합니다.

</br>

#### 5.4. 배치 작업 상태 조회

현재 실행 중인 배치 작업(장소, 주차장)이 있는지 상태를 확인합니다.

```http
GET /admin/batch/db/status
```

**응답 예시:**

```json
{
  "success": true,
  "data": {
    "parkingRunning": true,
    "placeRunning": false
  },
  "timestamp": "2025-10-15T12:00:00.000Z"
}
```

</br>

### 6. Elasticsearch 관리 API (`/admin/es`)

Elasticsearch 인덱스를 관리합니다.

#### 6.1. 인덱스 생성

`places`, `places_autocomplete` 등 서비스에 필요한 Elasticsearch 인덱스를 생성합니다.

```http
POST /admin/es/indices
```

**응답 예시:**

```json
{
  "success": true,
  "data": "인덱스 생성 절차가 성공적으로 완료되었습니다.",
  "timestamp": "2025-10-09T12:00:00.000Z"
}
```

**주의사항:**

- 데이터 색인 전에 반드시 실행되어야 합니다.

</br>

#### 6.2. 인덱스 삭제

모든 관련 Elasticsearch 인덱스를 삭제합니다.

```http
DELETE /admin/es/indices
```

**응답 예시:**

```json
{
  "success": true,
  "data": "인덱스 삭제 절차가 성공적으로 완료되었습니다.",
  "timestamp": "2025-10-09T12:00:00.000Z"
}
```

**주의사항:**

- **경고:** 인덱스에 저장된 모든 데이터가 영구적으로 삭제됩니다.

</br>

### 7. Elasticsearch 배치 관리 API (`/admin/batch/es`)

CSV 파일의 데이터를 Elasticsearch로 색인하는 배치 작업을 관리합니다.

#### 7.1. 전체 CSV 데이터 색인 (단계적 실행)

`data/csv/` 경로의 모든 CSV 파일을 읽어 `places` 인덱스에 단계적으로 색인합니다.

```http
POST /admin/batch/es/all-csv
```

**응답 예시:**

```json
{
  "success": true,
  "data": "CSV to ES 단계적 전체 재구성 배치 작업이 백그라운드에서 시작되었습니다.",
  "timestamp": "2025-10-09T12:00:00.000Z"
}
```

**주의사항:**

- Elasticsearch 클러스터 부하를 줄이기 위해 CSV 파일을 작은 그룹(5개)으로 나누어 순차적으로 처리하며, 각 그룹 처리 후 10초의 대기 시간이 있습니다.

</br>

#### 7.2. 증분 CSV 데이터 색인

`data/new-csv/` 경로의 신규 CSV 파일을 읽어 `places` 인덱스에 증분 색인합니다.

```http
POST /admin/batch/es/new-csv
```

**응답 예시:**

```json
{
  "success": true,
  "data": "CSV to ES 증분 추가 배치 작업이 백그라운드에서 시작되었습니다.",
  "timestamp": "2025-10-09T12:00:00.000Z"
}
```

</br>

#### 7.3. 주차장 인덱스 생성

`parkinglots` 인덱스를 Elasticsearch에 생성합니다.

```http
POST /admin/batch/es/parkinglots-index
```

**응답 예시:**
```json
{
  "success": true,
  "data": "인덱스 생성 성공",
  "timestamp": "2025-10-16T12:00:00.000Z"
}
```
**주의사항:**
- 주차장 데이터 색인 전에 반드시 한 번 실행되어야 합니다.
- 인덱스가 이미 존재하면 아무 작업도 수행하지 않습니다.

</br>

#### 7.4. 주차장 CSV 데이터 색인

`application.yml`에 설정된 경로의 주차장 CSV 파일을 읽어 `parkinglots` 인덱스에 색인합니다.

```http
POST /admin/batch/es/parkinglots-csv
```

**응답 예시:**
```json
{
  "success": true,
  "data": "ParkingLot CSV to ES 배치 작업이 백그라운드에서 시작되었습니다.",
  "timestamp": "2025-10-16T12:00:00.000Z"
}
```
**주의사항:**
- 이 작업은 백그라운드에서 비동기적으로 실행됩니다.
- 실행 전, `application.yml`의 `batch.parking.csv.file` 경로가 올바른지, 대상 CSV 파일의 첫 번째 열이 `id`이고 고유한 숫자 값이 채워져 있는지 확인해야 합니다.

</br>

### 8. 지오코딩 백필 API (`/admin/geocode`)

데이터베이스에 좌표(`location`)가 없는 주소 데이터에 대해 지오코딩(주소->좌표 변환)을 재시도합니다.

```http
POST /admin/geocode/backfill
```

**Query Parameters:**

- `limit` (integer, 선택): 한 번에 처리할 최대 데이터 수 (기본값: 500)

**응답 예시:**

```json
{
  "success": true,
  "data": "backfill accepted: ok=500",
  "timestamp": "2025-10-15T12:00:00.000Z"
}
```

**주의사항:**

- 이 작업은 카카오 API를 호출하므로, API 호출량 제한에 유의해야 합니다.

</br>

---

## 클라이언트 API (`/api`)

### 1. 장소 검색 (DB 기반)

> **[참고]** 이 API는 DB를 직접 조회하며, 더 빠른 성능의 Elasticsearch 기반 검색 API(`GET /api/searches`) 사용을 권장합니다.

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

### 2. 장소 검색 (Elasticsearch 기반)

Elasticsearch를 사용하여 키워드와 위치 기반으로 소비쿠폰 사용 가능 장소를 검색합니다.

```http
GET /api/searches
```

**Query Parameters:**

- `keyword` (string, 필수): 검색 키워드 (예: "마트", "편의점", "카페")
- `x` (double, 필수): 중심점 경도 (X 좌표)
- `y` (double, 필수): 중심점 위도 (Y 좌표)
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
        "roadAddress": "서울특별시 종로구 청계천로 400",
        "roadAddressDong": "종로동",
        "lotAddress": "서울특별시 종로구 종로5가 82-1",
        "lat": 37.5700,
        "lng": 127.0017,
        "phone": "02-1234-5678",
        "categoryGroupName": "대형마트",
        "placeUrl": "http://place.map.kakao.com/1234567890",
        "distance": 521.5
      }
    ],
    "pageable": {
      ...
    }
  },
  "timestamp": "2025-10-09T12:00:00.000Z"
}
```

</br>

### 3. 장소 상세 정보 조회 (DB 기반)

> **[참고]** 이 API는 DB를 직접 조회하며, 더 빠른 성능의 Elasticsearch 기반 상세 조회 API(`GET /api/searches/{id}`) 사용을 권장합니다.

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

### 4. 장소 상세 정보 조회 (Elasticsearch 기반)

Elasticsearch에서 특정 장소의 상세 정보를 조회합니다. 현재 사용자의 위치를 함께 보내면 거리가 계산되어 응답에 포함됩니다.

```http
GET /api/searches/{id}
```

**Path Parameters:**

- `id` (string, 필수): 장소 ID

**Query Parameters:**

- `x` (double, 선택): 사용자 현재 위치 경도
- `y` (double, 선택): 사용자 현재 위치 위도

**응답 예시:**

```json
{
  "success": true,
  "data": {
    "placeId": "1234567890",
    "placeName": "롯데마트 종로점",
    "roadAddress": "서울특별시 종로구 청계천로 400",
    "roadAddressDong": "종로동",
    "lotAddress": "서울특별시 종로구 종로5가 82-1",
    "lat": 37.5700,
    "lng": 127.0017,
    "phone": "02-1234-5678",
    "categoryGroupName": "대형마트",
    "placeUrl": "http://place.map.kakao.com/1234567890",
    "distance": 521.5
  },
  "timestamp": "2025-10-09T12:00:00.000Z"
}
```

</br>

### 5. 검색어 자동완성

입력된 검색어에 대한 자동완성 제안 목록을 제공합니다.

```http
GET /api/suggestions
```

**Query Parameters:**

- `word` (string, 필수): 자동완성을 위한 검색어 (2자 이상)

**응답 예시:**

```json
{
  "success": true,
  "data": [
    {
      "word": "롯데리아"
    },
    {
      "word": "롯데마트"
    },
    {
      "word": "롯데백화점"
    }
  ],
  "timestamp": "2025-10-09T12:00:00.000Z"
}
```

</br>

</br>

### 6. 주차장 검색 (Elasticsearch 기반) <- 이거 사용

Elasticsearch를 사용하여 좌표 기반으로 주변 주차장을 검색합니다.

```http
GET /api/searches/parkinglots-es
```

**Query Parameters:**

- `lat` (double, **필수**): 검색 중심 위도
- `lng` (double, **필수**): 검색 중심 경도
- `query` (string, 선택): 주차장 이름 등으로 결과를 필터링하기 위한 검색어
- `radius` (double, 선택): 검색 반경 (km 단위, 기본값: 1.0)
- `page` (integer, 선택): 페이지 번호 (기본값: 1)
- `size` (integer, 선택): 페이지 크기 (기본값: 10)
- `sort` (string, 선택): 정렬 기준 (`distance`: 거리순, `accuracy`: 관련도순)

**응답 예시:**

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 123,
        "parkingLotNm": "강남역 공영 주차장",
        "roadAddress": "서울 강남구 강남대로 396",
        "lotAddress": "서울 강남구 역삼동 825",
        "parkingChargeInfo": "유료",
        "lat": 37.4979,
        "lng": 127.0276,
        "distance": 150.0
      }
    ],"currentPage": 1,
    "totalPages": 1,
    "totalElements": 13,
    "startPage": 1,
    "endPage": 1,
    "hasPrev": false,
    "hasNext": false,
    "blockSize": 100,
    "searchCenterLat": 33.544825,
    "searchCenterLng": 126.662191,
    "last": true,
    "first": true
  },
  "timestamp": "2025-10-16T15:31:35.972927392"
}
```

</br>

### 7. 주차장 상세 정보 조회 <- 이거 사용

> **[정보]** 이 API는 이제 데이터베이스가 아닌 **Elasticsearch**에서 데이터를 조회합니다.

특정 주차장의 상세 정보를 조회합니다.

```http
GET /api/parkinglots/{id}
```

**Path Parameters:**

- `id` (long, 필수): 주차장 ID

**응답 예시:**

```json
{
  "success": true,
  "data": {
    "id": 123,
    "parkingLotNm": "강남역 공영 주차장",
    "roadAddress": "서울 강남구 강남대로 396",
    "lotAddress": "서울 강남구 역삼동 825",
    "parkingCapacity": "150",
    "operDay": "평일+토요일+공휴일",
    "weekOpenTime": "09:00",
    "weekCloseTime": "23:00",
    "satOpenTime": "09:00",
    "satCloseTime": "23:00",
    "holidayOpenTime": "09:00",
    "holidayCloseTime": "23:00",
    "parkingChargeInfo": "유료",
    "paymentMethod": "카드",
    "specialComment": "장애인 전용 구역 있음",
    "phoneNumber": "02-123-4567",
    "lat": 37.4979,
    "lng": 127.0276
  },
  "timestamp": "2025-10-15T12:00:00.000Z"
}
```

</br>

### 8. 주변 주차장 검색 (Elasticsearch 연동, 사용 X)

장소 검색어 또는 좌표를 기준으로 주변 주차장을 검색합니다. 요청 파라미터에 따라 두 가지 방식으로 동작합니다.

```http

GET /api/searches/parkinglots

```

**Query Parameters:**


- `query`(string, 선택) : 중심점을 찾기 위한 장소 검색어 (예: "강남역"). `lat`, `lng`이 없을 경우 필수.
- `lat`(double, 선택) : 검색의 중심이 될 위도. `query`가 없을 경우 `lng`과 함께 필수.
- `lng`(double, 선택) : 검색의 중심이 될 경도. `query`가 없을 경우 `lat`과 함께 필수.
- `page`(integer, 선택) : 조회할 페이지 번호 (기본값: 1)
- `userLat`(double, 선택) : **거리 계산**의 기준이 될 사용자의 실제 위도.
- `userLng`(double, 선택) : **거리 계산**의 기준이 될 사용자의 실제 경도.



**동작 방식:**
- **`query` 사용 시**: Elasticsearch로 장소를 검색해 중심 좌표를 찾은 후, 그 주변의 주차장을 검색합니다.
- **`lat`, `lng` 사용 시**: Elasticsearch 검색을 건너뛰고, 해당 좌표 주변의 주차장을 즉시 검색합니다.



**응답 예시:**
```json

{
  "success": true,
  "data": {
    "content": [
      {
        "id": 123,
        "parkingLotName": "강남역 공영 주차장",
        "roadAddress": "서울 강남구 강남대로 396",
        "lotAddress": "서울 강남구 역삼동 825",
        "feeInfo": "유료",
        "lat": 37.4979,
        "lng": 127.0276,
        "distance": 150.0
      }
    ],
    "currentPage": 1,
    "totalPages": 1,
    "totalElements": 1
  },
  "timestamp": "2025-10-15T12:00:00.000Z"
}

```

</br>

### 9. 주변 주차장 검색 (좌표 기반, 사용 X)

좌표와 반경을 기준으로 검색한 장소의 주변 주차장을 검색합니다. (DB 직접 조회)

```http
GET /api/parkinglots/nearby
```

**Query Parameters:**

- `lat` (double, 필수): 중심점 위도
- `lng` (double, 필수): 중심점 경도
- `radius` (double, 선택): 검색 반경 (km 단위, 기본값: 1.0)
- `page` (integer, 선택): 페이지 번호 (기본값: 1)
- `size` (integer, 선택): 페이지 크기 (기본값: 10)

**응답 예시:**

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 123,
        "parkingLotName": "강남역 공영 주차장",
        "roadAddress": "서울 강남구 강남대로 396",
        "lotAddress": "서울 강남구 역삼동 825",
        "feeInfo": "유료",
        "lat": 37.4979,
        "lng": 127.0276,
        "distance": 150.0
      }
    ],
    "currentPage": 1,
    "totalPages": 1,
    "totalElements": 1
  },
  "timestamp": "2025-10-15T12:00:00.000Z"
}
```

</br>



### 10. (구) 텍스트/좌표 기반 주변 주차장 검색 (사용 X)

> **[참고]** 이 API는 초기 개발 버전입니다. 장소 이름 기반 검색은 `GET /api/searches/parkinglots` 사용을 권장합니다.

텍스트 쿼리(주소) 또는 좌표를 기반으로 주변 주차장을 검색합니다.

```http
GET /api/search/parkinglots
```

**Query Parameters:**

- `query` (string, 선택): 주소 검색어
- `lat` (double, 선택): 위도
- `lng` (double, 선택): 경도
- `radius` (double, 선택): 검색 반경 (km 단위, 기본값: 1.0)
- `page` (integer, 선택): 페이지 번호 (기본값: 1)

**응답 예시:**
(응답 형식은 `GET /api/parkinglots/nearby` 와 동일합니다.)

</br>

### 11. 길찾기 API

출발지와 도착지 사이의 경로를 조회합니다. 하이브리드 시스템으로 카카오 API(자동차)와 OSRM(도보/자전거/대중교통)을 지원합니다.

```http
GET /api/routes/search
```

**Query Parameters:**

- `startLng` (double, 필수): 출발지 경도 (한국 좌표 범위: 124.0 ~ 132.0)
- `startLat` (double, 필수): 출발지 위도 (한국 좌표 범위: 33.0 ~ 39.0)
- `endLng` (double, 필수): 도착지 경도 (한국 좌표 범위: 124.0 ~ 132.0)
- `endLat` (double, 필수): 도착지 위도 (한국 좌표 범위: 33.0 ~ 39.0)
- `routeType` (string, 선택): 경로 타입 (기본값: "driving")
  - `driving`: 자동차 경로 (카카오 API 사용)
  - `walking`: 도보 경로 (OSRM 사용)
  - `cycling`: 자전거 경로 (OSRM 사용)
  - `transit`: 대중교통 경로 (OSRM 사용)

**요청 예시:**

```bash
curl -X GET "http://localhost:8080/api/routes/search?startLng=127.0276&startLat=37.4979&endLng=127.0286&endLat=37.4989&routeType=driving"
```

**응답 예시 (자동차 경로):**

```json
{
  "success": true,
  "data": {
    "coordinates": [
      {
        "lon": 127.027591411983,
        "lat": 37.497886868699254
      },
      {
        "lon": 127.02774914062023,
        "lat": 37.49793321971287
      }
    ],
    "totalDistance": 977.0,
    "totalDuration": 419.0,
    "routeType": "DRIVING",
    "steps": [
      {
        "instruction": "출발지",
        "distance": 0.0,
        "duration": 0.0,
        "startLocation": {
          "lon": 127.027591411983,
          "lat": 37.497886868699254
        },
        "endLocation": {
          "lon": 127.027591411983,
          "lat": 37.497886868699254
        }
      },
      {
        "instruction": "우회전",
        "distance": 252.0,
        "duration": 31.0,
        "startLocation": {
          "lon": 127.03027259286037,
          "lat": 37.498692826801836
        },
        "endLocation": {
          "lon": 127.03027259286037,
          "lat": 37.498692826801836
        }
      }
    ]
  },
  "timestamp": "2025-09-29T18:55:01.718578918"
}
```

**응답 예시 (도보/자전거 경로):**

```json
{
  "success": true,
  "data": {
    "coordinates": [
      {
        "lon": 127.02761,
        "lat": 37.49789
      },
      {
        "lon": 127.0286,
        "lat": 37.49891
      }
    ],
    "totalDistance": 429.2,
    "totalDuration": 61.6,
    "routeType": "WALKING",
    "steps": [
      {
        "instruction": null,
        "distance": 11.7,
        "duration": 5.6,
        "startLocation": {
          "lon": 127.027606,
          "lat": 37.497887
        },
        "endLocation": {
          "lon": 127.027606,
          "lat": 37.497887
        }
      }
    ]
  },
  "timestamp": "2025-09-29T18:56:00.471269293"
}
```

**에러 응답:**

```json
{
  "success": false,
  "error": {
    "code": "R006",
    "message": "유효하지 않은 좌표입니다"
  },
  "timestamp": "2025-09-29T18:55:01.718578918"
}
```

</br>

### 12. 경로 제공자 목록 조회 API

사용 가능한 경로 제공자 목록을 조회합니다.

```http
GET /api/routes/providers
```

</br>

### 에러 코드

- `R001`: 지원하지 않는 경로 타입
- `R002`: 경로 제공자 서비스가 일시적으로 사용할 수 없음
- `R003`: 경로를 찾을 수 없음
- `R004`: 카카오 API 서비스 오류가 발생했습니다
- `R005`: OSRM 서비스 오류가 발생했습니다
- `R006`: 유효하지 않은 좌표입니다

</br>

### 13. 길찾기 모니터링 API

길찾기 API의 성능 및 통계 정보를 조회합니다.

#### 13.1. API 호출 통계 조회

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

#### 13.2. 제공자 헬스체크

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

#### 13.3. 성능 메트릭 조회

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

**마지막 업데이트**: 2025-10-16  
**문서 버전**: 1.1.3
