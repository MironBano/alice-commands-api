@echo off
chcp 65001 >nul
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0_launcher.ps1" -Action push-catalog
exit /b %ERRORLEVEL%
