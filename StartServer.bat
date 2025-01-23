@echo off
cd /d "%~dp0"
echo Starting "docker-compose up --build"...
start cmd /k "docker-compose up --build -d"