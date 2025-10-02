#!/bin/bash

# OSRM 서버 테스트 스크립트

echo "OSRM 서버 테스트를 시작합니다..."

# 테스트 좌표 (서울 시청 -> 명동)
START_LNG="126.978652"
START_LAT="37.566826"
END_LNG="126.985161"
END_LAT="37.563569"

echo "테스트 경로: 서울시청 -> 명동"
echo "출발지: $START_LNG, $START_LAT"
echo "도착지: $END_LNG, $END_LAT"
echo ""

# 도보 경로 테스트
echo "도보 경로 테스트 (포트 5001)"
curl -s "http://localhost:5001/route/v1/foot/$START_LNG,$START_LAT;$END_LNG,$END_LAT" | jq '.routes[0].legs[0].summary' 2>/dev/null || echo "도보 경로 서버 응답 없음"

echo ""

# 자전거 경로 테스트
echo "자전거 경로 테스트 (포트 5002)"
curl -s "http://localhost:5002/route/v1/bike/$START_LNG,$START_LAT;$END_LNG,$END_LAT" | jq '.routes[0].legs[0].summary' 2>/dev/null || echo "자전거 경로 서버 응답 없음"

echo ""

# 대중교통 경로 테스트
echo "대중교통 경로 테스트 (포트 5003)"
curl -s "http://localhost:5003/route/v1/transit/$START_LNG,$START_LAT;$END_LNG,$END_LAT" | jq '.routes[0].legs[0].summary' 2>/dev/null || echo "대중교통 경로 서버 응답 없음"

echo ""
echo "OSRM 서버 테스트 완료!"
