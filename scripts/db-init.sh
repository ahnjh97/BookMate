#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

echo "주의: 기존 BookMate 테이블과 데이터가 삭제되고 다시 생성됩니다."
read -r -p "계속하려면 INIT을 입력하세요: " confirmation
if [[ "$confirmation" != "INIT" ]]; then
  echo "DB 초기화를 취소했습니다."
  exit 0
fi

if command -v mvn >/dev/null 2>&1; then
  MAVEN_CMD="mvn"
else
  MAVEN_CMD=$(find .tools -path '*/bin/mvn' -type f -print -quit 2>/dev/null || true)
fi

if [[ -z "${MAVEN_CMD:-}" ]]; then
  echo "Maven을 찾을 수 없습니다. Maven을 설치하거나 IntelliJ에서 DBInit.java를 실행하세요."
  exit 1
fi

"$MAVEN_CMD" -f backend/pom.xml compile exec:java -Dexec.mainClass=util.DBInit
