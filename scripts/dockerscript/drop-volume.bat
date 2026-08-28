@echo off
cd /d "%~dp0..\.."

docker volume rm bookmate_oracle-data 2>nul

docker compose down -v
echo Reset complete
pause