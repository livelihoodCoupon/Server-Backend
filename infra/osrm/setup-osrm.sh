#!/bin/bash

# OSRM 서버 설정 스크립트
# 한국 OpenStreetMap 데이터를 다운로드하고 OSRM 서버를 구축합니다.

set -e

echo "OSRM 서버 설정을 시작합니다..."

# 디렉토리 생성
mkdir -p ./data

# 한국 OSM 데이터 다운로드 (약 1GB)
echo "한국 OpenStreetMap 데이터를 다운로드합니다..."
cd ./data

if [ ! -f "south-korea-latest.osm.pbf" ]; then
    echo "다운로드 중... (시간이 오래 걸릴 수 있습니다)"
    # wget -O south-korea-latest.osm.pbf https://download.geofabrik.de/asia/south-korea-latest.osm.pbf # Windows에서는 curl 대신 wget 사용
    curl -L -o south-korea-latest.osm.pbf https://download.geofabrik.de/asia/south-korea-latest.osm.pbf
else
    echo "이미 다운로드된 파일이 있습니다."
fi

# OSRM 데이터 추출 및 준비 (car, foot, bike, transit 프로파일)
echo "OSRM 데이터 추출 및 준비 중... (시간이 매우 오래 걸릴 수 있습니다)"

# car 프로파일
echo "car 프로파일 데이터 생성 중..."
docker run --rm -v $(pwd):/data osrm/osrm-backend osrm-extract -p /opt/car.lua /data/south-korea-latest.osm.pbf
docker run --rm -v $(pwd):/data osrm/osrm-backend osrm-partition /data/south-korea-latest.osrm
docker run --rm -v $(pwd):/data osrm/osrm-backend osrm-customize /data/south-korea-latest.osrm
mv south-korea-latest.osrm.osrm south-korea-latest.osrm.car
mv south-korea-latest.osrm.osrm.edges south-korea-latest.osrm.car.edges
mv south-korea-latest.osrm.osrm.geometry south-korea-latest.osrm.car.geometry
mv south-korea-latest.osrm.osrm.properties south-korea-latest.osrm.car.properties
mv south-korea-latest.osrm.osrm.names south-korea-latest.osrm.car.names
mv south-korea-latest.osrm.osrm.nodes south-korea-latest.osrm.car.nodes
mv south-korea-latest.osrm.osrm.restrictions south-korea-latest.osrm.car.restrictions
mv south-korea-latest.osrm.osrm.tls south-korea-latest.osrm.car.tls
mv south-korea-latest.osrm.osrm.tld south-korea-latest.osrm.car.tld
mv south-korea-latest.osrm.osrm.cnbg south-korea-latest.osrm.car.cnbg
mv south-korea-latest.osrm.osrm.cnbl south-korea-latest.osrm.car.cnbl
mv south-korea-latest.osrm.osrm.ebg south-korea-latest.osrm.car.ebg
mv south-korea-latest.osrm.osrm.hsgr south-korea-latest.osrm.car.hsgr
mv south-korea-latest.osrm.osrm.m5m south-korea-latest.osrm.car.m5m
mv south-korea-latest.osrm.osrm.nbg south-korea-latest.osrm.car.nbg
mv south-korea-latest.osrm.osrm.nbl south-korea-latest.osrm.car.nbl
mv south-korea-latest.osrm.osrm.tgr south-korea-latest.osrm.car.tgr
mv south-korea-latest.osrm.osrm.file_index south-korea-latest.osrm.car.file_index
echo "car 프로파일 데이터 생성 완료."

# foot 프로파일
echo "foot 프로파일 데이터 생성 중..."
docker run --rm -v $(pwd):/data osrm/osrm-backend osrm-extract -p /opt/foot.lua /data/south-korea-latest.osm.pbf
docker run --rm -v $(pwd):/data osrm/osrm-backend osrm-partition /data/south-korea-latest.osrm
docker run --rm -v $(pwd):/data osrm/osrm-backend osrm-customize /data/south-korea-latest.osrm
mv south-korea-latest.osrm.osrm south-korea-latest.osrm.foot
mv south-korea-latest.osrm.osrm.edges south-korea-latest.osrm.foot.edges
mv south-korea-latest.osrm.osrm.geometry south-korea-latest.osrm.foot.geometry
mv south-korea-latest.osrm.osrm.properties south-korea-latest.osrm.foot.properties
mv south-korea-latest.osrm.osrm.names south-korea-latest.osrm.foot.names
mv south-korea-latest.osrm.osrm.nodes south-korea-latest.osrm.foot.nodes
mv south-korea-latest.osrm.osrm.restrictions south-korea-latest.osrm.foot.restrictions
mv south-korea-latest.osrm.osrm.tls south-korea-latest.osrm.foot.tls
mv south-korea-latest.osrm.osrm.tld south-korea-latest.osrm.foot.tld
mv south-korea-latest.osrm.osrm.cnbg south-korea-latest.osrm.foot.cnbg
mv south-korea-latest.osrm.osrm.cnbl south-korea-latest.osrm.foot.cnbl
mv south-korea-latest.osrm.osrm.ebg south-korea-latest.osrm.foot.ebg
mv south-korea-latest.osrm.osrm.hsgr south-korea-latest.osrm.foot.hsgr
mv south-korea-latest.osrm.osrm.m5m south-korea-latest.osrm.foot.m5m
mv south-korea-latest.osrm.osrm.nbg south-korea-latest.osrm.foot.nbg
mv south-korea-latest.osrm.osrm.nbl south-korea-latest.osrm.foot.nbl
mv south-korea-latest.osrm.osrm.tgr south-korea-latest.osrm.foot.tgr
mv south-korea-latest.osrm.osrm.file_index south-korea-latest.osrm.foot.file_index
echo "foot 프로파일 데이터 생성 완료."

# bike 프로파일
echo "bike 프로파일 데이터 생성 중..."
docker run --rm -v $(pwd):/data osrm/osrm-backend osrm-extract -p /opt/bike.lua /data/south-korea-latest.osm.pbf
docker run --rm -v $(pwd):/data osrm/osrm-backend osrm-partition /data/south-korea-latest.osrm
docker run --rm -v $(pwd):/data osrm/osrm-backend osrm-customize /data/south-korea-latest.osrm
mv south-korea-latest.osrm.osrm south-korea-latest.osrm.bike
mv south-korea-latest.osrm.osrm.edges south-korea-latest.osrm.bike.edges
mv south-korea-latest.osrm.osrm.geometry south-korea-latest.osrm.bike.geometry
mv south-korea-latest.osrm.osrm.properties south-korea-latest.osrm.bike.properties
mv south-korea-latest.osrm.osrm.names south-korea-latest.osrm.bike.names
mv south-korea-latest.osrm.osrm.nodes south-korea-latest.osrm.bike.nodes
mv south-korea-latest.osrm.osrm.restrictions south-korea-latest.osrm.bike.restrictions
mv south-korea-latest.osrm.osrm.tls south-korea-latest.osrm.bike.tls
mv south-korea-latest.osrm.osrm.tld south-korea-latest.osrm.bike.tld
mv south-korea-latest.osrm.osrm.cnbg south-korea-latest.osrm.bike.cnbg
mv south-korea-latest.osrm.osrm.cnbl south-korea-latest.osrm.bike.cnbl
mv south-korea-latest.osrm.osrm.ebg south-korea-latest.osrm.bike.ebg
mv south-korea-latest.osrm.osrm.hsgr south-korea-latest.osrm.bike.hsgr
mv south-korea-latest.osrm.osrm.m5m south-korea-latest.osrm.bike.m5m
mv south-korea-latest.osrm.osrm.nbg south-korea-latest.osrm.bike.nbg
mv south-korea-latest.osrm.osrm.nbl south-korea-latest.osrm.bike.nbl
mv south-korea-latest.osrm.osrm.tgr south-korea-latest.osrm.bike.tgr
mv south-korea-latest.osrm.osrm.file_index south-korea-latest.osrm.bike.file_index
echo "bike 프로파일 데이터 생성 완료."

# transit 프로파일 (foot 프로파일을 기반으로 사용)
echo "transit 프로파일 데이터 생성 중 (foot 프로파일 기반)..."
cp south-korea-latest.osrm.foot south-korea-latest.osrm.transit
cp south-korea-latest.osrm.foot.edges south-korea-latest.osrm.transit.edges
cp south-korea-latest.osrm.foot.geometry south-korea-latest.osrm.transit.geometry
cp south-korea-latest.osrm.foot.properties south-korea-latest.osrm.transit.properties
cp south-korea-latest.osrm.foot.names south-korea-latest.osrm.transit.names
cp south-korea-latest.osrm.foot.nodes south-korea-latest.osrm.transit.nodes
cp south-korea-latest.osrm.foot.restrictions south-korea-latest.osrm.transit.restrictions
cp south-korea-latest.osrm.foot.tls south-korea-latest.osrm.transit.tls
cp south-korea-latest.osrm.foot.tld south-korea-latest.osrm.transit.tld
cp south-korea-latest.osrm.foot.cnbg south-korea-latest.osrm.transit.cnbg
cp south-korea-latest.osrm.foot.cnbl south-korea-latest.osrm.transit.cnbl
cp south-korea-latest.osrm.foot.ebg south-korea-latest.osrm.transit.ebg
cp south-korea-latest.osrm.foot.hsgr south-korea-latest.osrm.transit.hsgr
cp south-korea-latest.osrm.foot.m5m south-korea-latest.osrm.transit.m5m
cp south-korea-latest.osrm.foot.nbg south-korea-latest.osrm.transit.nbg
cp south-korea-latest.osrm.foot.nbl south-korea-latest.osrm.transit.nbl
cp south-korea-latest.osrm.foot.tgr south-korea-latest.osrm.transit.tgr
cp south-korea-latest.osrm.foot.file_index south-korea-latest.osrm.transit.file_index
echo "transit 프로파일 데이터 생성 완료."

echo "OSRM 설정이 완료되었습니다!"
echo "이제 다음 명령어로 OSRM 서버를 시작할 수 있습니다:"
echo "docker-compose up osrm"
