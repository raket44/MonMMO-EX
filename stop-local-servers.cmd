@echo off
rem findstr treats a quoted pattern with a space as SEVERAL patterns unless /C: is given: the old
rem `findstr ":7777 .*LISTENING"` matched every line containing ":7777" OR any LISTENING line, so
rem it killed the client connected to the VPS's port 7777 and every listening process on the
rem machine (2026-09-08). /R /C: makes it one regex: our own LISTENING socket only.
for /f "tokens=5" %%p in ('netstat -ano ^| findstr /R /C:"TCP.*:2106 .*LISTENING"') do taskkill /PID %%p /F
for /f "tokens=5" %%p in ('netstat -ano ^| findstr /R /C:"TCP.*:7777 .*LISTENING"') do taskkill /PID %%p /F
echo Requested shutdown for local MonMMO server processes on ports 2106 and 7777.
