# 프론트엔드 개발 가이드 - 길찾기 기능

> 소비쿠폰 시스템의 길찾기 기능을 위한 프론트엔드 개발 가이드입니다.

## 개요

### 구현된 기능
- **하이브리드 길찾기 시스템**: 카카오 API(자동차) + OSRM(도보/자전거/대중교통)
- **RESTful API**: 표준 HTTP 메서드 사용
- **실시간 경로 조회**: 출발지/도착지 좌표 기반 경로 계산

### 지원하는 경로 타입
| 타입 | 설명 | 제공자 | 특징 |
|------|------|--------|------|
| `DRIVING` | 자동차 | 카카오 API | 턴바이턴 안내, 정확한 도로 정보 |
| `WALKING` | 도보 | OSRM | 보행자 최적화 경로 |
| `CYCLING` | 자전거 | OSRM | 자전거 도로 우선 경로 |
| `TRANSIT` | 대중교통 | OSRM | 대중교통 연결 경로 |

---

## API 엔드포인트

### Base URL
```
http://localhost:8080
```

### 1. 경로 조회 API
```http
GET /api/routes/search
```

**Query Parameters:**
- `startLng` (number): 출발지 경도
- `startLat` (number): 출발지 위도  
- `endLng` (number): 도착지 경도
- `endLat` (number): 도착지 위도
- `routeType` (string): 경로 타입 (DRIVING, WALKING, CYCLING, TRANSIT)

**예시 요청:**
```javascript
const response = await fetch(
  'http://localhost:8080/api/routes/search?' + 
  'startLng=127.027619&startLat=37.497942&' +
  'endLng=127.028619&endLat=37.498942&' +
  'routeType=DRIVING'
);
```

### 2. 제공자 목록 조회 API
```http
GET /api/routes/providers
```

**응답:**
```json
["KakaoNavi", "OSRM"]
```

---

## 응답 데이터 구조

### 경로 조회 응답
```typescript
interface RouteResponse {
  coordinates: Coordinate[];      // 경로 좌표 배열
  totalDistance: number;          // 총 거리 (미터)
  totalDuration: number;          // 총 소요시간 (초)
  routeType: RouteType;           // 경로 타입
  steps: RouteStep[];            // 상세 경로 단계
}

interface Coordinate {
  lng: number;                   // 경도
  lat: number;                   // 위도
}

interface RouteStep {
  instruction: string | null;     // 안내 문구
  distance: number;              // 단계별 거리 (미터)
  duration: number;              // 단계별 소요시간 (초)
  startLocation: Coordinate;      // 시작 위치
  endLocation: Coordinate;       // 종료 위치
}

type RouteType = 'DRIVING' | 'WALKING' | 'CYCLING' | 'TRANSIT';
```

### 실제 응답 예시

**자동차 경로:**
```json
{
  "coordinates": [
    {"lng": 127.02759059417166, "lat": 37.497949935830974},
    {"lng": 127.02766823190767, "lat": 37.49806771206318}
  ],
  "totalDistance": 420.0,
  "totalDuration": 148.0,
  "routeType": "DRIVING",
  "steps": [
    {
      "instruction": "출발지",
      "distance": 0.0,
      "duration": 0.0,
      "startLocation": {"lng": 127.02759059417166, "lat": 37.497949935830974},
      "endLocation": {"lng": 127.02759059417166, "lat": 37.497949935830974}
    },
    {
      "instruction": "우회전",
      "distance": 190.0,
      "duration": 57.0,
      "startLocation": {"lng": 127.02697034078221, "lat": 37.49955771701745},
      "endLocation": {"lng": 127.02697034078221, "lat": 37.49955771701745}
    }
  ]
}
```

**도보 경로:**
```json
{
  "coordinates": [
    {"lng": 127.027641, "lat": 37.497897},
    {"lng": 127.02863, "lat": 37.498918}
  ],
  "totalDistance": 429.1,
  "totalDuration": 61.9,
  "routeType": "WALKING",
  "steps": [
    {
      "instruction": null,
      "distance": 8.4,
      "duration": 5.5,
      "startLocation": {"lng": 127.027641, "lat": 37.497897},
      "endLocation": {"lng": 127.027641, "lat": 37.497897}
    }
  ]
}
```

---

## 프론트엔드 구현 가이드

### 1. 기본 API 호출 함수
```typescript
// 경로 조회 함수
async function getRoute(
  startLng: number,
  startLat: number,
  endLng: number,
  endLat: number,
  routeType: RouteType
): Promise<RouteResponse> {
  const params = new URLSearchParams({
    startLng: startLng.toString(),
    startLat: startLat.toString(),
    endLng: endLng.toString(),
    endLat: endLat.toString(),
    routeType
  });

  const response = await fetch(`http://localhost:8080/api/routes/search?${params}`);
  
  if (!response.ok) {
    throw new Error(`HTTP error! status: ${response.status}`);
  }
  
  return await response.json();
}

// 제공자 목록 조회 함수
async function getProviders(): Promise<string[]> {
  const response = await fetch('http://localhost:8080/api/routes/providers');
  return await response.json();
}
```

### 2. 에러 처리
```typescript
try {
  const route = await getRoute(127.027619, 37.497942, 127.028619, 37.498942, 'DRIVING');
  console.log('경로 조회 성공:', route);
} catch (error) {
  console.error('경로 조회 실패:', error);
  // 사용자에게 에러 메시지 표시
}
```

### 3. React 컴포넌트 예시
```tsx
import React, { useState, useEffect } from 'react';

interface RouteSearchProps {
  startLocation: { lng: number; lat: number };
  endLocation: { lng: number; lat: number };
}

const RouteSearch: React.FC<RouteSearchProps> = ({ startLocation, endLocation }) => {
  const [route, setRoute] = useState<RouteResponse | null>(null);
  const [routeType, setRouteType] = useState<RouteType>('DRIVING');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const searchRoute = async () => {
    setLoading(true);
    setError(null);
    
    try {
      const result = await getRoute(
        startLocation.lng,
        startLocation.lat,
        endLocation.lng,
        endLocation.lat,
        routeType
      );
      setRoute(result);
    } catch (err) {
      setError('경로를 찾을 수 없습니다.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <select 
        value={routeType} 
        onChange={(e) => setRouteType(e.target.value as RouteType)}
      >
        <option value="DRIVING">자동차</option>
        <option value="WALKING">도보</option>
        <option value="CYCLING">자전거</option>
        <option value="TRANSIT">대중교통</option>
      </select>
      
      <button onClick={searchRoute} disabled={loading}>
        {loading ? '검색 중...' : '경로 찾기'}
      </button>
      
      {error && <div style={{color: 'red'}}>{error}</div>}
      
      {route && (
        <div>
          <h3>경로 정보</h3>
          <p>거리: {(route.totalDistance / 1000).toFixed(2)}km</p>
          <p>소요시간: {Math.round(route.totalDuration / 60)}분</p>
          <p>경로 타입: {route.routeType}</p>
          
          <h4>상세 경로</h4>
          <ul>
            {route.steps.map((step, index) => (
              <li key={index}>
                {step.instruction || `단계 ${index + 1}`} - 
                {step.distance}m ({Math.round(step.duration / 60)}분)
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
};
```

### 4. 지도 표시 (카카오맵 예시)
```typescript
// 카카오맵에 경로 표시
function displayRouteOnMap(map: kakao.maps.Map, route: RouteResponse) {
  // 기존 경로 제거
  if (window.routePolyline) {
    window.routePolyline.setMap(null);
  }
  
  // 좌표 배열 생성
  const path = route.coordinates.map(coord => 
    new kakao.maps.LatLng(coord.lat, coord.lng)
  );
  
  // 경로 선 그리기
  const polyline = new kakao.maps.Polyline({
    path: path,
    strokeWeight: 5,
    strokeColor: getRouteColor(route.routeType),
    strokeOpacity: 0.7,
    strokeStyle: 'solid'
  });
  
  polyline.setMap(map);
  window.routePolyline = polyline;
}

function getRouteColor(routeType: RouteType): string {
  switch (routeType) {
    case 'DRIVING': return '#FF6B6B';
    case 'WALKING': return '#4ECDC4';
    case 'CYCLING': return '#45B7D1';
    case 'TRANSIT': return '#96CEB4';
    default: return '#FF6B6B';
  }
}
```

---

## UI/UX 권장사항

### 1. 경로 타입 선택 UI
```tsx
const RouteTypeSelector = ({ value, onChange }: { value: RouteType; onChange: (type: RouteType) => void }) => (
  <div className="route-type-selector">
    {[
      { type: 'DRIVING', label: '🚗 자동차', color: '#FF6B6B' },
      { type: 'WALKING', label: '🚶 도보', color: '#4ECDC4' },
      { type: 'CYCLING', label: '🚴 자전거', color: '#45B7D1' },
      { type: 'TRANSIT', label: '🚌 대중교통', color: '#96CEB4' }
    ].map(({ type, label, color }) => (
      <button
        key={type}
        className={`route-type-btn ${value === type ? 'active' : ''}`}
        style={{ borderColor: color }}
        onClick={() => onChange(type as RouteType)}
      >
        {label}
      </button>
    ))}
  </div>
);
```

### 2. 경로 정보 표시
```tsx
const RouteInfo = ({ route }: { route: RouteResponse }) => (
  <div className="route-info">
    <div className="route-summary">
      <span className="distance">
        📏 {(route.totalDistance / 1000).toFixed(2)}km
      </span>
      <span className="duration">
        ⏱️ {Math.round(route.totalDuration / 60)}분
      </span>
    </div>
    
    <div className="route-steps">
      {route.steps.map((step, index) => (
        <div key={index} className="route-step">
          <div className="step-number">{index + 1}</div>
          <div className="step-content">
            <div className="step-instruction">
              {step.instruction || `단계 ${index + 1}`}
            </div>
            <div className="step-details">
              {step.distance}m • {Math.round(step.duration / 60)}분
            </div>
          </div>
        </div>
      ))}
    </div>
  </div>
);
```

### 3. 로딩 상태
```tsx
const LoadingSpinner = () => (
  <div className="loading-container">
    <div className="spinner"></div>
    <p>경로를 찾는 중...</p>
  </div>
);
```

---

## 주의사항

### 1. API 호출 제한
- **동시 요청**: 너무 많은 동시 요청은 서버 부하를 일으킬 수 있습니다
- **캐싱**: 동일한 경로 요청은 캐시에서 반환되므로 빠릅니다
- **에러 처리**: 네트워크 오류나 서버 오류에 대한 적절한 처리 필요

### 2. 좌표 시스템
- **WGS84 좌표계** 사용 (경도/위도)
- **정밀도**: 소수점 6자리까지 지원
- **범위**: 한국 내 좌표만 지원

### 3. 성능 최적화
```typescript
// 디바운싱을 통한 API 호출 최적화
const useDebounce = (value: any, delay: number) => {
  const [debouncedValue, setDebouncedValue] = useState(value);
  
  useEffect(() => {
    const handler = setTimeout(() => {
      setDebouncedValue(value);
    }, delay);
    
    return () => {
      clearTimeout(handler);
    };
  }, [value, delay]);
  
  return debouncedValue;
};
```

---

## 테스트 데이터

### 서울 시청 ↔ 강남역
```typescript
const testRoutes = {
  seoulCityHall: { lng: 126.978652, lat: 37.566826 },
  gangnam: { lng: 127.027619, lat: 37.497942 }
};

// 테스트 호출
const testRoute = await getRoute(
  testRoutes.seoulCityHall.lng,
  testRoutes.seoulCityHall.lat,
  testRoutes.gangnam.lng,
  testRoutes.gangnam.lat,
  'DRIVING'
);
```

---

## 추가 리소스

- [API 사용 가이드](./API_USAGE_GUIDE.md) - 상세 API 문서
- [카카오맵 JavaScript API](https://apis.map.kakao.com/web/guide/)
- [OSRM 공식 문서](https://project-osrm.org/docs/v5.24.0/api/)

---

**문서 버전**: 1.0.0  
**마지막 업데이트**: 2025-09-25
