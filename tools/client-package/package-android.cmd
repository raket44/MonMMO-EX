@echo off
rem Builds the MonMMO-EX Android client from a retail PokeMMO APK.
rem   package-android.cmd <server-host> [retail.apk] [out.apk]
rem Same redirect as package-client.cmd: the client trusts our game/chat keys, dials <server-host>
rem for login (2106) and game (7777), ignores the retail feed and enables our expansion mod.
rem The signing key lives in %USERPROFILE%\.monmmo\android-signing (never in the repo). Keep it:
rem an APK signed with a different key cannot install over an earlier one.
setlocal
if "%~1"=="" (
  echo usage: package-android.cmd ^<server-host^> [retail.apk] [out.apk]
  exit /b 2
)
set "HOST=%~1"
set "APK=%~2"
if "%APK%"=="" set "APK=%USERPROFILE%\Downloads\pokemmo-r32645.apk"
set "OUT=%~3"
if "%OUT%"=="" set "OUT=%USERPROFILE%\Downloads\MonMMO-EX-Android.apk"
set "REPO=%~dp0..\.."
set "CLIENT=%LOCALAPPDATA%\MonMMO-EX\Client-31914"
set "JDK=C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot\bin"
set "KEYDIR=%USERPROFILE%\.monmmo\android-signing"
set "KEYSTORE=%KEYDIR%\monmmo-ex.p12"
set "PASSFILE=%KEYDIR%\password.txt"
set "MOD=%CLIENT%\data\mods\monmmo-lost-knights.zip"
if not exist "%MOD%" set "MOD=-"

if not exist "%APK%" (
  echo ERROR: retail APK not found: %APK%
  exit /b 1
)
if not exist "%KEYSTORE%" (
  echo ERROR: signing keystore missing: %KEYSTORE%
  exit /b 1
)

set "WORK=%TEMP%\monmmo-android-build"
if exist "%WORK%" rd /s /q "%WORK%"
mkdir "%WORK%"

echo Patching %APK% (server keys, config, mod)...
"%JDK%\java.exe" -Xmx2g "%~dp0ApkPackager.java" prepare "%APK%" "%WORK%\unsigned.apk" "%HOST%" "%MOD%" "%REPO%\launcher\src\main\resources\game.public.pem" "%REPO%\launcher\src\main\resources\chat.public.pem" || exit /b 1

echo Signing (v1)...
"%JDK%\jarsigner.exe" -keystore "%KEYSTORE%" -storetype PKCS12 -storepass:file "%PASSFILE%" -sigalg SHA256withRSA -digestalg SHA-256 -signedjar "%WORK%\v1.apk" "%WORK%\unsigned.apk" monmmo >nul || exit /b 1

echo Aligning and signing (v2)...
"%JDK%\java.exe" -Xmx2g "%~dp0ApkPackager.java" finish "%WORK%\v1.apk" "%OUT%" "%KEYSTORE%" "%PASSFILE%" monmmo || exit /b 1

"%JDK%\jarsigner.exe" -verify "%OUT%" >nul || (
  echo ERROR: v1 signature does not verify on %OUT%
  exit /b 1
)
rd /s /q "%WORK%"
echo Done: %OUT%
