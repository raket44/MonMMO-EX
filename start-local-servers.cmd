@echo off
setlocal
cd

set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot"
set "PATH=%JAVA_HOME%\bin;C:\Program Files\Docker\Docker\resources\bin;%PATH%"
set "OPENMMO_DEV_TOOLS=true"
rem Generated Expansion species are installed in Client-31914, so the server may hand them out.
set "OPENMMO_EXPANSION_CLIENT_CONTENT=true"

docker info >nul 2>&1
if errorlevel 1 (
  echo Starting the local database service...
  if not exist "C:\Program Files\Docker\Docker\Docker Desktop.exe" (
    echo Docker Desktop is not installed in the expected location.
    pause
    exit /b 1
  )
  powershell.exe -NoProfile -Command "Start-Process -FilePath 'C:\Program Files\Docker\Docker\Docker Desktop.exe' -WindowStyle Hidden"
  for /l %%I in (1,1,45) do (
    docker info >nul 2>&1
    if not errorlevel 1 goto docker_ready
    ping 127.0.0.1 -n 3 >nul
  )
  echo Docker Desktop did not finish starting.
  pause
  exit /b 1
)

:docker_ready
docker compose up -d
if errorlevel 1 (
  echo The local databases could not be started.
  pause
  exit /b 1
)
ping 127.0.0.1 -n 4 >nul

if not exist logs mkdir logs
del /q logs\server-login.log logs\server-login.err.log logs\server-game.log logs\server-game.err.log 2>nul

start "MonMMO Login Server" /min cmd /c "call gradlew.bat :server.login:run > logs\server-login.log 2> logs\server-login.err.log"
ping 127.0.0.1 -n 6 >nul
start "MonMMO Game Server" /min cmd /c "call gradlew.bat :server.game:run > logs\server-game.log 2> logs\server-game.err.log"

echo Started MonMMO login and game server tasks.
echo Logs:
echo   logs\server-login.log
echo   logs\server-login.err.log
echo   logs\server-game.log
echo   logs\server-game.err.log
