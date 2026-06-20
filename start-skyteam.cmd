@echo off
setlocal
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0start-skyteam.ps1"
if errorlevel 1 (
    echo.
    echo Start fehlgeschlagen. Details stehen oben oder in backend\target\logs.
    pause
)
endlocal
