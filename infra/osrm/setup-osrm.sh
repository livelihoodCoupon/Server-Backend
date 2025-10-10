#!/bin/bash

echo "OSRM 서버 설정을 시작합니다..."

# 한국 OpenStreetMap 데이터 다운로드
if [ ! -f "data/south-korea-latest.osm.pbf" ]; then
    echo "한국 OpenStreetMap 데이터를 다운로드합니다..."
    mkdir -p data
    curl -L -o data/south-korea-latest.osm.pbf https://download.geofabrik.de/asia/south-korea-latest.osm.pbf
    echo "다운로드 완료!"
else
    echo "이미 다운로드된 파일이 있습니다."
fi

echo "OSRM 데이터 추출 및 준비 중... (시간이 매우 오래 걸릴 수 있습니다)"

# car 프로파일 데이터 생성
echo "car 프로파일 데이터 생성 중..."
mkdir -p data/temp_car
cp data/south-korea-latest.osm.pbf data/temp_car/
docker run --rm -v $(pwd)/data/temp_car:/data osrm/osrm-backend osrm-extract -p /opt/car.lua /data/south-korea-latest.osm.pbf
docker run --rm -v $(pwd)/data/temp_car:/data osrm/osrm-backend osrm-partition /data/south-korea-latest.osrm
docker run --rm -v $(pwd)/data/temp_car:/data osrm/osrm-backend osrm-customize /data/south-korea-latest.osrm
# car 프로파일용 파일들 복사
cp data/temp_car/south-korea-latest.osrm.osrm data/south-korea-latest.car.osrm
for file in data/temp_car/south-korea-latest.osrm.*; do
    if [[ $file != *.pbf ]]; then
        cp "$file" "data/south-korea-latest.car.osrm${file#data/temp_car/south-korea-latest.osrm}"
    fi
done
rm -rf data/temp_car
echo "car 프로파일 데이터 생성 완료."

# foot 프로파일 데이터 생성
echo "foot 프로파일 데이터 생성 중..."
mkdir -p data/temp_foot
cp data/south-korea-latest.osm.pbf data/temp_foot/
docker run --rm -v $(pwd)/data/temp_foot:/data osrm/osrm-backend osrm-extract -p /opt/foot.lua /data/south-korea-latest.osm.pbf
docker run --rm -v $(pwd)/data/temp_foot:/data osrm/osrm-backend osrm-partition /data/south-korea-latest.osrm
docker run --rm -v $(pwd)/data/temp_foot:/data osrm/osrm-backend osrm-customize /data/south-korea-latest.osrm
# foot 프로파일용 파일들 복사
cp data/temp_foot/south-korea-latest.osrm.osrm data/south-korea-latest.foot.osrm
for file in data/temp_foot/south-korea-latest.osrm.*; do
    if [[ $file != *.pbf ]]; then
        cp "$file" "data/south-korea-latest.foot.osrm${file#data/temp_foot/south-korea-latest.osrm}"
    fi
done
rm -rf data/temp_foot
echo "foot 프로파일 데이터 생성 완료."

# bike 프로파일 데이터 생성
echo "bike 프로파일 데이터 생성 중..."
mkdir -p data/temp_bike
cp data/south-korea-latest.osm.pbf data/temp_bike/
docker run --rm -v $(pwd)/data/temp_bike:/data osrm/osrm-backend osrm-extract -p /opt/bicycle.lua /data/south-korea-latest.osm.pbf
docker run --rm -v $(pwd)/data/temp_bike:/data osrm/osrm-backend osrm-partition /data/south-korea-latest.osrm
docker run --rm -v $(pwd)/data/temp_bike:/data osrm/osrm-backend osrm-customize /data/south-korea-latest.osrm
# bike 프로파일용 파일들 복사
cp data/temp_bike/south-korea-latest.osrm.osrm data/south-korea-latest.bike.osrm
for file in data/temp_bike/south-korea-latest.osrm.*; do
    if [[ $file != *.pbf ]]; then
        cp "$file" "data/south-korea-latest.bike.osrm${file#data/temp_bike/south-korea-latest.osrm}"
    fi
done
rm -rf data/temp_bike
echo "bike 프로파일 데이터 생성 완료."

echo "OSRM 설정이 완료되었습니다!"
echo "이제 다음 명령어로 OSRM 서버를 시작할 수 있습니다:"
echo "docker-compose up -d"
