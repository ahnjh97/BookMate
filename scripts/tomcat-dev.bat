@echo off
chcp 65001 >nul
setlocal EnableExtensions

for %%I in ("%~dp0..") do set "PROJECT_ROOT=%%~fI"

echo Starting BookMate in development mode...
echo Frontend HTML, CSS, and JavaScript changes are applied after browser refresh.
echo If this script cannot find Maven or Tomcat, run tomcat-run.bat once first.
echo.

powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%PROJECT_ROOT%\scripts\tomcat-dev.ps1"
set "SCRIPT_EXIT=%ERRORLEVEL%"

echo.
if not "%SCRIPT_EXIT%"=="0" (
  echo BookMate development server did not start. Review the error above.
) else (
  echo BookMate development server ended normally.
)
pause

endlocal & exit /b %SCRIPT_EXIT%
