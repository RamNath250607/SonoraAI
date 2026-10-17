@echo off
title SONORA AI - Desktop Launcher
echo ====================================================
echo           SONORA AI - Smart Music Player
echo ====================================================
echo.

cd /d "%~dp0"

echo [1/2] Checking SONORA AI services on Port 8080...
powershell -Command "try { $r = Invoke-WebRequest -Uri 'http://localhost:8080/health' -TimeoutSec 2 -UseBasicParsing; exit 0 } catch { exit 1 }" >nul 2>&1
if %errorlevel% neq 0 (
    echo Services are offline. Starting SONORA AI backend & frontend...
    docker info >nul 2>&1
    if %errorlevel% equ 0 (
        echo Starting via Docker Compose...
        docker compose up -d
    ) else (
        echo Starting via Native Python Services...
        powershell -Command "Start-Process python -ArgumentList '-m uvicorn main:app --app-dir backend --host 0.0.0.0 --port 8080' -WindowStyle Hidden; Start-Process python -ArgumentList '-m http.server 3000 --directory frontend' -WindowStyle Hidden"
    )
    timeout /t 3 /nobreak >nul
) else (
    echo SONORA AI services are online and ready!
)

echo.
echo [2/2] Launching SONORA AI in Desktop Window Mode...

if exist "C:\Program Files\Google\Chrome\Application\chrome.exe" (
    start "" "C:\Program Files\Google\Chrome\Application\chrome.exe" --app=http://localhost:3000 --window-size=1280,840
) else if exist "C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe" (
    start "" "C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe" --app=http://localhost:3000 --window-size=1280,840
) else (
    start http://localhost:3000
)

echo.
echo SONORA AI is active! Enjoy your music.
echo ====================================================
