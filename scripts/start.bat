@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion
cd /d "%~dp0.."

docker compose up -d
for /f %%i in ('docker compose ps -q oracle-db') do set CID=%%i

REM healthy 상태 대기중, 백그라운드 초기화 로그
REM /k 로 창을 유지시키고, 로그 스트림 중단(Ctrl+C) 후 안내 문구를 띄워 사용자가 종료
start "DB 초기화 로그 (확인 후 아무 키나 눌러 닫으세요)" cmd /k "docker compose logs -f oracle-db & echo. & echo [로그 확인 완료 시 아무 키나 눌러 이 창을 닫으세요] & pause >nul"

for /l %%n in (1,1,48) do (
  for /f %%s in ('docker inspect --format="{{.State.Health.Status}}" !CID! 2^>nul') do set STATUS=%%s
  if "!STATUS!"=="healthy" (
    echo ready
    pause
    exit /b 0
  )
  timeout /t 5 /nobreak >nul
)

echo timeout: docker compose logs oracle-db 확인
pause
exit /b 1
