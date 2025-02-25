@echo off
cd /d "%~dp0"
echo Starting "docker-compose up --build"...
start cmd /c "docker-compose up --build -d"
exit
