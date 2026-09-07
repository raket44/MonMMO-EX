@echo off
rem Stops the Gradle and Kotlin build daemons: they hold gigabytes of heap between builds, which
rem starved a 13 GB machine running the servers and the client (2026-09-06). Run after every build.
call "%~dp0..\gradlew.bat" --stop >nul 2>&1
for /f "tokens=2 delims=," %%p in ('wmic process where "name='java.exe' and commandline like '%%KotlinCompileDaemon%%'" get processid /format:csv 2^>nul ^| findstr /r "[0-9]"') do taskkill /pid %%p /f >nul 2>&1
echo Build daemons stopped.
