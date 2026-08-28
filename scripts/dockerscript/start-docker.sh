#!/usr/bin/env bash
set -e
cd "$(dirname "$0")/../.."   # scripts/ → 프로젝트 루트 (docker-compose.yml 위치)

docker compose up -d
CID=$(docker compose ps -q oracle-db)

for i in $(seq 1 48); do
  STATUS=$(docker inspect --format='{{.State.Health.Status}}' "$CID" 2>/dev/null)
  if [ "$STATUS" = "healthy" ]; then
    echo "DB healthy. Initializing schema/seed via DBInit..."
    ./backend/mvnw -f backend/pom.xml compile exec:java -Dexec.mainClass=util.DBInit
    echo "ready"
    exit 0
  fi
  sleep 5
done

echo "timeout: docker compose logs oracle-db 확인"
exit 1