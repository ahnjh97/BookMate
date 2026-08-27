@echo off
setlocal EnableExtensions
chcp 65001 >nul
pushd "%~dp0"

where javac.exe >nul 2>nul
if errorlevel 1 (
  echo [ERROR] javac was not found. Install a JDK and add it to PATH.
  popd
  pause
  exit /b 1
)

where java.exe >nul 2>nul
if errorlevel 1 (
  echo [ERROR] java was not found. Install a JDK and add it to PATH.
  popd
  pause
  exit /b 1
)

echo Compiling DevProxyServer...
javac -encoding UTF-8 DevProxyServer.java
if errorlevel 1 (
  echo [ERROR] DevProxyServer compilation failed.
  popd
  pause
  exit /b 1
)

echo.
echo Starting the proxy server. Press Ctrl+C to stop.
java -Dfile.encoding=UTF-8 DevProxyServer
set "PROXY_EXIT=%ERRORLEVEL%"

popd
if not "%PROXY_EXIT%"=="0" pause
endlocal & exit /b %PROXY_EXIT%
