@echo off
cd /d "%~dp0distributed_system_terminal"
echo Starting "npm run dev"...
start cmd /k "npm run dev"

cd /d "%~dp0servers_terminal"
echo Starting "gradle bootrun"...
start cmd /k "gradle bootrun"
