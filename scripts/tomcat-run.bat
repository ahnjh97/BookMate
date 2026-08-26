@echo off
chcp 65001 >nul
setlocal EnableExtensions

for %%I in ("%~dp0..") do set "PROJECT_ROOT=%%~fI"

echo Starting BookMate without IntelliJ Tomcat integration...
echo The first run may download Maven and Tomcat into .tools.
echo.

powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%PROJECT_ROOT%\scripts\tomcat-run.ps1"
set "SCRIPT_EXIT=%ERRORLEVEL%"

echo.
if not "%SCRIPT_EXIT%"=="0" (
  echo BookMate did not start. Review the error above.
) else (
  echo BookMate Tomcat process ended normally.
)
pause

endlocal & exit /b %SCRIPT_EXIT%
