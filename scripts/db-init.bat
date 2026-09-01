@echo off
setlocal EnableExtensions
chcp 65001 >nul
cd /d "%~dp0.."

echo WARNING: Existing BookMate tables and data will be deleted and recreated.
set /p "CONFIRM=Type I to continue: "
if /i not "%CONFIRM%"=="I" (
  echo Database initialization cancelled.
  exit /b 0
)
if not exist "backend\mvnw.cmd" (
  echo backend\mvnw.cmd was not found. Pull the latest repository files and try again.
  pause
  exit /b 1
)

call backend\mvnw.cmd -q -f backend\pom.xml compile exec:java -Dexec.mainClass=util.DBInit -Dfile.encoding=UTF-8
set "SCRIPT_EXIT=%ERRORLEVEL%"
echo.
pause
endlocal & exit /b %SCRIPT_EXIT%
