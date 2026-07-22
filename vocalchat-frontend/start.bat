@echo off
cd /d "%~dp0"
nginx -p . -c nginx.conf
echo VocalChat Frontend: http://localhost
pause
