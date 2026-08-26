@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion
cd /d "%~dp0.."

docker compose up -d
for /f %%i in ('docker compose ps -q oracle-db') do set CID=%%i

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
