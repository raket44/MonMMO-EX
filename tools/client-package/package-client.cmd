@echo off
rem Builds the portable MonMMO-EX client folder a friend can unzip and play from.
rem   package-client.cmd <server-host> [out-dir]
rem <server-host> is the address the client dials for both login (2106) and game (7777):
rem your public IP with the router forwarding those ports, or your Tailscale/VPN IP.
rem Rebuild whenever patch-classes.jar, data.pak/strings or the expansion mod change.
setlocal
if "%~1"=="" (
  echo usage: package-client.cmd ^<server-host^> [out-dir]
  exit /b 2
)
set "HOST=%~1"
set "OUT=%~2"
if "%OUT%"=="" set "OUT=%USERPROFILE%\Downloads\MonMMO-EX-Client"
set "CLIENT=%LOCALAPPDATA%\MonMMO-EX\Client-31914"
set "JAVA=C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot\bin\java.exe"
if not exist "%JAVA%" set "JAVA=java"

echo Building %OUT%\MonMMO-EX.exe (retail launcher stub + patched client jar + overlay)...
"%JAVA%" "%~dp0BuildClientExe.java" "%CLIENT%\PokeMMO.exe" "%CLIENT%\MonMMO-Local.exe" "%CLIENT%\patch-classes.jar" "%OUT%\MonMMO-EX.exe" || exit /b 1
copy /y "%CLIENT%\MonMMO-Local.l4j.ini" "%OUT%\MonMMO-EX.l4j.ini" >nul
copy /y "%CLIENT%\revision.txt" "%OUT%\" >nul
copy /y "%CLIENT%\pokemmo_updater.jar" "%OUT%\" >nul

echo Copying the bundled Java runtime and game data (this takes a minute)...
robocopy "%CLIENT%\jre" "%OUT%\jre" /E /NFL /NDL /NJH /NJS /NP >nul
robocopy "%CLIENT%\data" "%OUT%\data" /E /XD mods /NFL /NDL /NJH /NJS /NP >nul
if not exist "%OUT%\data\mods" mkdir "%OUT%\data\mods"
if not exist "%OUT%\config" mkdir "%OUT%\config"
if not exist "%OUT%\roms" mkdir "%OUT%\roms"
copy /y "%CLIENT%\data\mods\monmmo-lost-knights.zip" "%OUT%\data\mods\" >nul

rem The zzz- name makes this file load after main.properties, so it wins.
(
  echo # MonMMO-EX: points the client at our server instead of the official one.
  echo force.ls.host=%HOST%
  echo force.ls.port=2106
  echo force.gs.host=%HOST%
  echo force.gs.port=7777
  echo client.misc.ignore_feed=true
  echo client.misc.testserver_feed_signature=false
  echo client.mods.enabled_mods=monmmo-lost-knights.zip
) > "%OUT%\config\zzz-monmmo-ex-server.properties"
echo client.mods.enabled_mods=monmmo-lost-knights.zip> "%OUT%\config\main.properties"
copy /y "%~dp0README-player.txt" "%OUT%\README.txt" >nul

echo Zipping...
if exist "%OUT%.zip" del "%OUT%.zip"
tar -a -c -f "%OUT%.zip" -C "%OUT%\.." "%~nx2"
if "%~2"=="" tar -a -c -f "%OUT%.zip" -C "%USERPROFILE%\Downloads" "MonMMO-EX-Client"
echo Done: %OUT%.zip
