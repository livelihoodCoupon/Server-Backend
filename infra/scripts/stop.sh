#!/usr/bin/env bash
# infra/scripts/stop.sh
# - infra/* 경로의 docker-compose.yml을 병렬로 종료
# - docker compose v2 / docker-compose(v1) 자동 감지
# - 선택적으로 볼륨 제거
set -euo pipefail

# ── 1️⃣ docker compose 명령어 감지 ──
if docker compose version >/dev/null 2>&1; then
  DOCKER_COMPOSE_CMD="docker compose"
elif docker-compose version >/dev/null 2>&1; then
  DOCKER_COMPOSE_CMD="docker-compose"
else
  echo "ERROR: docker compose 또는 docker-compose가 설치되어 있지 않습니다." >&2
  exit 1
fi

# ── 2️⃣ 경로 설정 ──
ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
INFRA_BASE_DIR="${ROOT_DIR}/infra"

# ── 3️⃣ compose 디렉토리 목록 (종료는 시작 역순) ──
COMPOSE_DIRS=(
  "${INFRA_BASE_DIR}/monitoring"
  "${INFRA_BASE_DIR}/elk"
  "${INFRA_BASE_DIR}/database"
)

# ── 4️⃣ 병렬 종료 ──
PIDS=()
for d in "${COMPOSE_DIRS[@]}"; do
  if [ -f "${d}/docker-compose.yml" ] || [ -f "${d}/docker-compose.yaml" ]; then
    echo "=== Stopping services in ${d} ==="
    pushd "${d}" >/dev/null

    if [ -f "${INFRA_BASE_DIR}/.env" ]; then
      $DOCKER_COMPOSE_CMD --env-file "${INFRA_BASE_DIR}/.env" down --timeout 10 --volumes &
    else
      $DOCKER_COMPOSE_CMD down --timeout 10 --volumes &
    fi

    PIDS+=($!)
    popd >/dev/null
  else
    echo "Skip: no docker-compose.yml in ${d}"
  fi
done

# ── 5️⃣ 모든 백그라운드 프로세스 대기 ──
for pid in "${PIDS[@]}"; do
  wait $pid
done

echo "모든 요청된 compose 스택이 병렬로 중지되었습니다."
