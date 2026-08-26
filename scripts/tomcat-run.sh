#!/usr/bin/env bash
set -euo pipefail

SCRIPT_PATH="${BASH_SOURCE[0]}"
if [[ "$SCRIPT_PATH" == */* ]]; then
  SCRIPT_PATH="${SCRIPT_PATH%/*}"
else
  SCRIPT_PATH="."
fi
SCRIPT_DIR="$(cd "$SCRIPT_PATH" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
TOMCAT_DIR="${1:-${CATALINA_HOME:-${TOMCAT_HOME:-}}}"

if [[ -z "$TOMCAT_DIR" ]]; then
  echo "[ERROR] Tomcat 경로를 찾을 수 없습니다."
  echo "사용법: bash scripts/tomcat-run.sh /path/to/apache-tomcat-10.1.x"
  echo "또는 CATALINA_HOME 환경변수를 설정해 주세요."
  exit 1
fi

if [[ ! -f "$TOMCAT_DIR/bin/catalina.sh" ]]; then
  echo "[ERROR] '$TOMCAT_DIR'은 올바른 Tomcat 설치 경로가 아닙니다."
  echo "bin/catalina.sh 파일이 있는 폴더를 지정해 주세요."
  exit 1
fi

if ! command -v mvn >/dev/null 2>&1; then
  echo "[ERROR] Maven 실행 파일을 찾을 수 없습니다."
  echo "Maven을 설치하고 mvn 명령을 PATH에 등록해 주세요."
  exit 1
fi

if [[ ! -f "$PROJECT_ROOT/.env" ]]; then
  echo "[WARN] 프로젝트 루트에 .env 파일이 없습니다."
  echo "DB_URL, DB_USER, DB_PASSWORD가 시스템 환경변수에 없다면 DB 연결에 실패합니다."
fi

echo "[1/3] BookMate WAR 파일을 빌드합니다."
mvn -f "$PROJECT_ROOT/backend/pom.xml" package

echo "[2/3] WAR 파일을 Tomcat에 배포합니다."
cp "$PROJECT_ROOT/backend/target/bookmate.war" "$TOMCAT_DIR/webapps/bookmate.war"

export BOOKMATE_ENV_DIR="$PROJECT_ROOT"

echo "[3/3] Tomcat을 시작합니다."
echo "접속 주소: http://localhost:8080/bookmate/"
echo "종료하려면 Ctrl+C를 누르세요."
echo

exec "$TOMCAT_DIR/bin/catalina.sh" run
