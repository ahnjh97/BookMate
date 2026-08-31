@echo off
REM BookMate backend (embedded Tomcat) - runs Main.java via Maven Wrapper
setlocal EnableExtensions
cd /d "%~dp0.."

echo Starting BookMate embedded Tomcat...
echo Local Oracle and the project-root .env file must be ready.
call backend\mvnw.cmd -f backend\pom.xml compile exec:java
set "SCRIPT_EXIT=%ERRORLEVEL%"
if not "%SCRIPT_EXIT%"=="0" pause
endlocal & exit /b %SCRIPT_EXIT%
