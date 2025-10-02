# OSRM 서버 설정 가이드

OSRM (Open Source Routing Machine) 서버를 설정하여 도보, 자전거, 자동차 경로를 제공합니다.

## 빠른 시작

### 1. OSRM 데이터 설정
```bash
# OSRM 데이터 다운로드 및 빌드 (약 1-2시간 소요)
cd infra/osrm
./setup-osrm.sh
```

**중요**: 이 스크립트는 각 프로파일별로 다른 데이터를 생성합니다:
- `car`: 자동차 전용 경로 (고속도로, 일방통행 고려)
- `foot`: 도보 전용 경로 (보행자 구역, 계단 고려)  
- `bike`: 자전거 전용 경로 (자전거 도로, 오르막길 고려)

### 2. OSRM 서버 시작
```bash
# OSRM 서버들 시작
docker-compose -f infra/osrm/docker-compose.yml up -d

# 또는 메인 docker-compose와 함께
docker-compose up -d
```

### 3. 서버 상태 확인
```bash
# OSRM 서버 상태 확인
curl http://localhost:5004/route/v1/car/127.027619,37.497942;127.028619,37.498942
curl http://localhost:5001/route/v1/foot/127.027619,37.497942;127.028619,37.498942
curl http://localhost:5002/route/v1/bike/127.027619,37.497942;127.028619,37.498942
```

## 디렉토리 구조

```
infra/osrm/
├── setup-osrm.sh          # OSRM 데이터 설정 스크립트
├── docker-compose.yml     # OSRM 서버 Docker Compose
├── README.md              # 이 파일
└── data/                  # OSRM 데이터 (자동 생성)
    ├── south-korea-latest.osm.pbf
    ├── south-korea-latest.osrm
    └── ...
```

## 설정 상세

### 포트 매핑
- **도보 경로**: `localhost:5001`  
- **자전거 경로**: `localhost:5002`
- **자동차 경로**: `localhost:5004`
- **대중교통 경로**: `localhost:5001` (foot과 동일한 결과 반환)

### 데이터 소스
- **OpenStreetMap**: 한국 데이터 (약 1GB)
- **업데이트**: 월 1회 (Geofabrik에서 자동 업데이트)

### 성능 최적화
- **MLD 알고리즘**: 빠른 경로 계산
- **메모리 사용량**: 약 2-4GB
- **디스크 사용량**: 약 3-5GB

## *** 문제 해결

### 1. 메모리 부족
```bash
# Docker 메모리 제한 확인
docker stats
```

### 2. 데이터 다운로드 실패
```bash
# 수동으로 데이터 다운로드
wget -O data/south-korea-latest.osm.pbf https://download.geofabrik.de/asia/south-korea-latest.osm.pbf
```

### 3. 서버 시작 실패
```bash
# 로그 확인
docker-compose -f infra/osrm/docker-compose.yml logs osrm-car
```

## 모니터링

### 서버 상태 확인
```bash
# OSRM 서버 헬스체크
curl http://localhost:5000/health
```

### 성능 테스트
```bash
# 경로 계산 시간 측정
time curl "http://localhost:5000/route/v1/driving/127.027619,37.497942;127.028619,37.498942"
```

## 업데이트

### 데이터 업데이트
```bash
# 새로운 OSM 데이터 다운로드
cd infra/osrm
rm -rf data/*
./setup-osrm.sh

# 서버 재시작
docker-compose -f infra/osrm/docker-compose.yml restart
```

### 팀원을 위한 데이터 공유
```bash
# 생성된 데이터를 압축하여 공유
cd infra/osrm
tar -czf osrm-data.tar.gz data/

# 팀원이 받은 후 압축 해제
tar -xzf osrm-data.tar.gz
```

**주의**: 데이터 파일은 약 2GB이므로 Git에 커밋하지 마세요. 별도로 공유하거나 클라우드 스토리지를 사용하세요.

## 참고 자료

- [OSRM 공식 문서](https://project-osrm.org/)
- [OpenStreetMap 데이터](https://www.openstreetmap.org/)
- [Geofabrik 다운로드](https://download.geofabrik.de/)
