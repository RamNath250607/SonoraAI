@echo off
title Ram Beats Launcher
cd /d "C:\Users\ramna\OneDrive\ドキュメント\Projects\sonora-ai"

:: Check if backend server is already active on port 8080
netstat -ano | findstr ":8080 " | findstr "LISTENING" >nul
if %errorlevel% neq 0 (
    echo Starting Ram Beats Engine on port 8080...
    start /b "" python -m uvicorn main:app --app-dir backend --host 0.0.0.0 --port 8080 >nul 2>&1
    timeout /t 2 /nobreak >nul
)

:: Open in dedicated App Window (Chrome or Edge or Default Browser)
if exist "C:\Program Files\Google\Chrome\Application\chrome.exe" (
    start "" "C:\Program Files\Google\Chrome\Application\chrome.exe" --app=http://localhost:8080
) else if exist "C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe" (
    start "" "C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe" --app=http://localhost:8080
) else (
    start http://localhost:8080
)
