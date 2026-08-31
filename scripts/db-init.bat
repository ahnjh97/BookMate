@echo off
setlocal EnableExtensions
cd /d "%~dp0.."

echo WARNING: Existing BookMate tables and data will be deleted and recreated.
set /p "CONFIRM=Type INIT to continue: "
if /i not "%CONFIRM%"=="INIT" (
  echo Database initialization cancelled.
  exit /b 0
)
@REM 아래 주석 해제시 예전 방식(시스템 PATH -> .tools 폴더 순으로 Maven 탐색)
@REM set "MAVEN_CMD="
@REM where mvn.cmd >nul 2>nul && set "MAVEN_CMD=mvn.cmd"
@REM if not defined MAVEN_CMD (
@REM   for /d %%D in ("%CD%\.tools\apache-maven-*") do if exist "%%~fD\bin\mvn.cmd" set "MAVEN_CMD=%%~fD\bin\mvn.cmd"
@REM )
@REM if not defined MAVEN_CMD (
@REM   echo Maven was not found. Install Maven or run DBInit.java from IntelliJ.
@REM   pause
@REM   exit /b 1
@REM )
if not exist "backend\mvnw.cmd" (
  echo backend\mvnw.cmd was not found. Pull the latest repo or run DBInit.java from IntelliJ.
  pause
  exit /b 1
)

@REM call "%MAVEN_CMD%" -f backend\pom.xml compile exec:java -Dexec.mainClass=util.DBInit
call backend\mvnw.cmd -f backend\pom.xml compile exec:java -Dexec.mainClass=util.DBInit
set "SCRIPT_EXIT=%ERRORLEVEL%"
if not "%SCRIPT_EXIT%"=="0" pause
endlocal & exit /b %SCRIPT_EXIT%
