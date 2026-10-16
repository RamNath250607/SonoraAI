@echo off
title SONORA AI - Desktop Launcher
echo ====================================================
echo           SONORA AI - Smart Music Player
echo ====================================================
echo.

cd /d "%~dp0"

echo [1/2] Checking SONORA AI Docker services...
docker compose ps --services --filter "status=running" | findstr /i "sonora-frontend" >nul 2>&1
if %errorlevel% neq 0 (
    echo Starting SONORA AI backend and frontend containers...
    docker compose up -d
    timeout /t 3 /nobreak >nul
) else (
    echo SONORA AI services are already online!
)

echo.
echo [2/2] Launching SONORA AI in Desktop Window Mode...

if exist "C:\Program Files\Google\Chrome\Application\chrome.exe" (
    start "" "C:\Program Files\Google\Chrome\Application\chrome.exe" --app=http://localhost:3000 --window-size=1280,820
) else if exist "C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe" (
    start "" "C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe" --app=http://localhost:3000 --window-size=1280,820
) else (
    start http://localhost:3000
)

echo.
echo SONORA AI is running! Enjoy your full-length music.
echo ====================================================
