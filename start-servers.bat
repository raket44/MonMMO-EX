@echo off
rem Starts the MonMMO login and game servers detached, each with its own console window and log.
rem Safe to re-run: each server refuses to bind if the port is already taken, and the window shows
rem why. Dev flags are set here so a plain double-click always gets the full dev feature set.
rem
rem Prefers the INSTALLED distribution (build\install\...) - a plain JVM start with no Gradle
rem daemon in front, so the ports open in ~2s instead of after a long Gradle prelude (which is
rem what made "connecting" feel slow). Refresh the dists after code changes with:
rem   gradlew.bat :server.game:installDist :server.login:installDist
rem Falls back to gradlew run when a dist has not been built yet.
cd /d "%~dp0"
set OPENMMO_DEV_TOOLS=true
set OPENMMO_EXPANSION_CLIENT_CONTENT=true

if exist "server.login\build\install\server.login\bin\server.login.bat" (
  start "MonMMO Login Server" cmd /k "cd /d "%~dp0server.login" && build\install\server.login\bin\server.login.bat"
) else (
  start "MonMMO Login Server" cmd /k "gradlew.bat :server.login:run --console=plain"
)

if exist "server.game\build\install\server.game\bin\server.game.bat" (
  start "MonMMO Game Server" cmd /k "cd /d "%~dp0server.game" && build\install\server.game\bin\server.game.bat"
) else (
  start "MonMMO Game Server" cmd /k "gradlew.bat :server.game:run --console=plain"
)
echo Both servers launching in their own windows. Close a window to stop that server.
