@echo off
for /f "tokens=5" %%p in ('netstat -ano ^| findstr ":2106 .*LISTENING"') do taskkill /PID %%p /F
for /f "tokens=5" %%p in ('netstat -ano ^| findstr ":7777 .*LISTENING"') do taskkill /PID %%p /F
echo Requested shutdown for local MonMMO server processes on ports 2106 and 7777.
