@echo off
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0_launcher.ps1" -Action pull-catalog
