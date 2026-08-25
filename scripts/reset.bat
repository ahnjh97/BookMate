@echo off
cd /d "%~dp0.."
docker compose down -v
echo 초기화 완료
pause