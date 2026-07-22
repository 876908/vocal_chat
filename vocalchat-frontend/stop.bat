@echo off
cd /d "%~dp0"
nginx -p . -c nginx.conf -s stop
echo Stopped.
pause
