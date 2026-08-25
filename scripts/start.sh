#!/usr/bin/env bash
set -e
cd "$(dirname "$0")/.."   # scripts/ → 프로젝트 루트 (docker-compose.yml 위치)

docker compose up -d
CID=$(docker compose ps -q oracle-db)

for i in $(seq 1 48); do
  STATUS=$(docker inspect --format='{{.State.Health.Status}}' "$CID" 2>/dev/null)
  [ "$STATUS" = "healthy" ] && { echo "ready"; exit 0; }
  sleep 5
done

echo "timeout: docker compose logs oracle-db 확인"
exit 1