@echo off
rem Stops the Gradle and Kotlin build daemons (see stop-build-daemons.ps1). Run after every build.
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0stop-build-daemons.ps1"
