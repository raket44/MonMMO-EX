@echo off
rem Builds the portable PiratedVersion client folder a friend can unzip and play from.
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
rem The project folder holds repo\ (this checkout) next to builds\.
for %%I in ("%~dp0..\..\..") do set "ROOT=%%~fI"
if "%OUT%"=="" set "OUT=%ROOT%\builds\PiratedVersion-Client"
set "CLIENT=%LOCALAPPDATA%\PiratedVersion\Client-31914"
set "JAVA=C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot\bin\java.exe"
if not exist "%JAVA%" set "JAVA=java"

rem Anything a previous run of the client left in the folder must not ship: its cache, logs, the
rem log folder, and a nested copy from unzipping into the same folder. ROMs are left alone.
if exist "%OUT%\cache" rd /s /q "%OUT%\cache"
if exist "%OUT%\log" rd /s /q "%OUT%\log"
if exist "%OUT%\PiratedVersion-Client" rd /s /q "%OUT%\PiratedVersion-Client"
del /q "%OUT%\*.log" 2>nul

echo Building %OUT%\PiratedVersion.exe (retail launcher stub + patched client jar + overlay)...
"%JAVA%" "%~dp0BuildClientExe.java" "%CLIENT%\PokeMMO.exe" "%CLIENT%\PiratedVersion-Local.exe" "%CLIENT%\patch-classes.jar" "%OUT%\PiratedVersion.exe" || exit /b 1
copy /y "%CLIENT%\PiratedVersion-Local.l4j.ini" "%OUT%\PiratedVersion.l4j.ini" >nul
copy /y "%CLIENT%\revision.txt" "%OUT%\" >nul
copy /y "%CLIENT%\pokemmo_updater.jar" "%OUT%\" >nul

echo Copying the bundled Java runtime and game data (this takes a minute)...
robocopy "%CLIENT%\jre" "%OUT%\jre" /E /NFL /NDL /NJH /NJS /NP >nul
robocopy "%CLIENT%\data" "%OUT%\data" /E /XD mods /NFL /NDL /NJH /NJS /NP >nul
if not exist "%OUT%\data\mods" mkdir "%OUT%\data\mods"
rem The config folder is rebuilt from scratch every run. The operator plays from this very folder,
rem so the client writes savedcredentials.properties (the account login) and the per-character
rem settings into it; zipping them shipped the admin login with the package (2026-09-09). Only the
rem two files written below may exist here.
if exist "%OUT%\config" rd /s /q "%OUT%\config"
mkdir "%OUT%\config"
if not exist "%OUT%\roms" mkdir "%OUT%\roms"
copy /y "%CLIENT%\data\mods\pirated-lost-knights.zip" "%OUT%\data\mods\" >nul

rem The zzz- name makes this file load after main.properties, so it wins.
(
  echo # PiratedVersion: points the client at our server instead of the official one.
  echo force.ls.host=%HOST%
  echo force.ls.port=2106
  echo force.gs.host=%HOST%
  echo force.gs.port=7777
  echo client.misc.ignore_feed=true
  echo client.misc.testserver_feed_signature=false
  echo client.mods.enabled_mods=pirated-lost-knights.zip
) > "%OUT%\config\zzz-pirated-server.properties"
echo client.mods.enabled_mods=pirated-lost-knights.zip> "%OUT%\config\main.properties"
copy /y "%~dp0README-player.txt" "%OUT%\README.txt" >nul

rem Hard stop: no login may ever leave this machine inside the package.
if exist "%OUT%\config\savedcredentials.properties" (
  echo ERROR: %OUT%\config\savedcredentials.properties exists - not zipping. & exit /b 1
)
echo Zipping...
if exist "%OUT%.zip" del "%OUT%.zip"
for %%D in ("%OUT%") do (set "OUTNAME=%%~nxD" & set "OUTPARENT=%%~dpD")
"%SystemRoot%\System32\tar.exe" -a -c -f "%OUT%.zip" -C "%OUTPARENT%." "%OUTNAME%"

echo Done: %OUT%.zip
