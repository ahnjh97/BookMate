#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

echo "BookMate 내장 Tomcat을 시작합니다."
echo "로컬 Oracle과 프로젝트 루트의 .env가 준비되어 있어야 합니다."

if command -v mvn >/dev/null 2>&1; then
  MAVEN_CMD="mvn"
else
  MAVEN_CMD=$(find .tools -path '*/bin/mvn' -type f -print -quit 2>/dev/null || true)
fi

if [[ -z "${MAVEN_CMD:-}" ]]; then
  echo "Maven을 찾을 수 없습니다. Maven을 설치하거나 IntelliJ에서 Main.java를 실행하세요."
  exit 1
fi

"$MAVEN_CMD" -f backend/pom.xml compile exec:java
