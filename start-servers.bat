@echo off
rem Starts the MonMMO login and game servers detached, each with its own console window and log.
rem Safe to re-run: each server refuses to bind if the port is already taken, and the window shows
rem why. Dev flags are set here so a plain double-click always gets the full dev feature set.
cd /d "%~dp0"
set OPENMMO_DEV_TOOLS=true
set OPENMMO_EXPANSION_CLIENT_CONTENT=true
start "MonMMO Login Server" cmd /k "gradlew.bat :server.login:run --console=plain"
start "MonMMO Game Server" cmd /k "gradlew.bat :server.game:run --console=plain"
echo Both servers launching in their own windows. Close a window to stop that server.
