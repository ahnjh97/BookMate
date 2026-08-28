@echo off
setlocal EnableExtensions
cd /d "%~dp0.."

echo WARNING: Existing BookMate tables and data will be deleted and recreated.
set /p "CONFIRM=Type INIT to continue: "
if /i not "%CONFIRM%"=="INIT" (
  echo Database initialization cancelled.
  exit /b 0
)

set "MAVEN_CMD="
where mvn.cmd >nul 2>nul && set "MAVEN_CMD=mvn.cmd"
if not defined MAVEN_CMD (
  for /d %%D in ("%CD%\.tools\apache-maven-*") do if exist "%%~fD\bin\mvn.cmd" set "MAVEN_CMD=%%~fD\bin\mvn.cmd"
)
if not defined MAVEN_CMD (
  if exist "backend\mvnw.cmd" set "MAVEN_CMD=backend\mvnw.cmd"
)
if not defined MAVEN_CMD (
  echo Maven was not found. Install Maven or run DBInit.java from IntelliJ.
  pause
  exit /b 1
)

call "%MAVEN_CMD%" -f backend\pom.xml compile exec:java -Dexec.mainClass=util.DBInit
set "SCRIPT_EXIT=%ERRORLEVEL%"
if not "%SCRIPT_EXIT%"=="0" pause
endlocal & exit /b %SCRIPT_EXIT%
