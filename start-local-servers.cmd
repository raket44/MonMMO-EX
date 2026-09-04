@echo off
setlocal
cd

set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot"
set "PATH=%JAVA_HOME%\bin;C:\Program Files\Docker\Docker\resources\bin;%PATH%"
set "OPENMMO_DEV_TOOLS=true"
rem Generated Expansion species are installed in Client-31914, so the server may hand them out.
set "OPENMMO_EXPANSION_CLIENT_CONTENT=true"

rem The gradle run tasks forwarded .env into the server environment; the direct dist launch must
rem do the same (DB credentials, session secret, game key) or login-to-game handoff breaks.
for /f "usebackq eol=# tokens=1,* delims==" %%A in ("%~dp0.env") do set "%%A=%%B"
rem The run tasks also forced the dev seed on; match that so Flyway keeps validating.
if not defined LOGIN_DB_SEED_DEV set "LOGIN_DB_SEED_DEV=true"
if not defined GAME_DB_SEED_DEV set "GAME_DB_SEED_DEV=true"
rem Jars exclude *.pem on purpose (keys.gradle.kts), so the dist launch must hand the key over
rem as a file path; the gradle run classpath carried it as a loose resource instead.
if not defined OPENMMO_GAME_PRIVATE_KEY if not defined OPENMMO_GAME_PRIVATE_KEY_FILE set "OPENMMO_GAME_PRIVATE_KEY_FILE=%~dp0server.game\src\main\resources\game.private.pem"

docker info >nul 2>&1
if errorlevel 1 (
  echo Starting the local database service...
  if not exist "C:\Program Files\Docker\Docker\Docker Desktop.exe" (
    echo Docker Desktop is not installed in the expected location.
    pause
    exit /b 1
  )
  powershell.exe -NoProfile -Command "Start-Process -FilePath 'C:\Program Files\Docker\Docker\Docker Desktop.exe' -WindowStyle Hidden"
  rem A cold Docker Desktop start regularly needs several minutes; the old 2-minute budget gave
  rem up mid-boot and stranded the Play button. Wait up to ~10 minutes, announcing progress.
  for /l %%I in (1,1,120) do (
    docker info >nul 2>&1
    if not errorlevel 1 goto docker_ready
    if %%I==24 echo Still waiting for Docker Desktop - a cold start can take a few minutes...
    ping 127.0.0.1 -n 6 >nul
  )
  echo Docker Desktop did not finish starting after 10 minutes.
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
rem Keep the previous run: battle diagnostics live in these and a restart used to wipe them.
for %%F in (server-login.log server-login.err.log server-game.log server-game.err.log) do if exist "logs\%%F" move /y "logs\%%F" "logs\prev-%%F" >nul

rem Prefer the INSTALLED distributions: a plain JVM start opens the ports in ~2s, instead of the
rem Gradle daemon prelude that made the client's "connecting..." hang after clicking log-in.
rem Java is invoked DIRECTLY with a wildcard classpath - the dists' generated .bat launchers
rem enumerate every jar and blow cmd's line-length limit ("The input line is too long").
rem Refresh the dists after server code changes:
rem   gradlew.bat :server.game:installDist :server.login:installDist
rem Working dir is each server's project dir - the hot-reload data files (warp-rules.txt,
rem nds-*.txt) resolve relative to it.
if exist "server.login\build\install\server.login\lib" (
  start "MonMMO Login Server" /min /d "%~dp0server.login" cmd /c ""%JAVA_HOME%\bin\java.exe" -cp "build\install\server.login\lib\*" de.fiereu.openmmo.server.login.MainKt > "%~dp0logs\server-login.log" 2> "%~dp0logs\server-login.err.log""
) else (
  start "MonMMO Login Server" /min cmd /c "call gradlew.bat :server.login:run > logs\server-login.log 2> logs\server-login.err.log"
)
ping 127.0.0.1 -n 3 >nul
if exist "server.game\build\install\server.game\lib" (
  start "MonMMO Game Server" /min /d "%~dp0server.game" cmd /c ""%JAVA_HOME%\bin\java.exe" "-Dmonmmo.retailData=%~dp0data\pokemmo\monsters.json" -cp "build\install\server.game\lib\*" de.fiereu.openmmo.server.game.MainKt > "%~dp0logs\server-game.log" 2> "%~dp0logs\server-game.err.log""
) else (
  start "MonMMO Game Server" /min cmd /c "call gradlew.bat :server.game:run > logs\server-game.log 2> logs\server-game.err.log"
)

echo Started MonMMO login and game server tasks.
echo Logs:
echo   logs\server-login.log
echo   logs\server-login.err.log
echo   logs\server-game.log
echo   logs\server-game.err.log
