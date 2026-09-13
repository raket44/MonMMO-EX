@echo off
rem Builds the MonMMO-EX Android client from a retail PokeMMO APK.
rem   package-android.cmd <server-host> [retail.apk] [out.apk]
rem Same redirect as package-client.cmd: the client trusts our game/chat keys, dials <server-host>
rem for login (2106) and game (7777), ignores the retail feed and enables our expansion mod.
rem The Expansion content (species, learnsets, details, names, sprites) is staged against this
rem APK's own r32645 data, so the client can resolve every species a party can hold.
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
set "JDK=C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot\bin"
set "KEYDIR=%USERPROFILE%\.monmmo\android-signing"
set "KEYSTORE=%KEYDIR%\monmmo-ex.p12"
set "PASSFILE=%KEYDIR%\password.txt"
set "TAR=%SystemRoot%\System32\tar.exe"

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

rem The stager reads <root>\data\data.pak and <root>\data\strings\strings_en.xml as its stock base.
echo Staging Expansion content against the APK's own data (a few minutes)...
set "SROOT=%WORK%\stock-root"
set "STAGE=%WORK%\stage"
mkdir "%SROOT%\data\strings"
"%TAR%" -xf "%APK%" -C "%WORK%" assets/data/data.pak assets/data/strings/strings_en.xml || exit /b 1
move /y "%WORK%\assets\data\data.pak" "%SROOT%\data\data.pak" >nul || exit /b 1
move /y "%WORK%\assets\data\strings\strings_en.xml" "%SROOT%\data\strings\strings_en.xml" >nul || exit /b 1
call "%REPO%\gradlew.bat" -p "%REPO%" :launcher:stageExpansionClientContent "-Pexpansion.clientRoot=%SROOT%" "-Pexpansion.outputDir=%STAGE%" --offline -q || exit /b 1
rem Retail stays retail: the build stops if the staged data changed any retail record in a way
rem that was not approved (learnsets may only gain moves, Fairy egg groups, four named strings).
echo Checking that retail content is preserved...
call "%REPO%\gradlew.bat" -p "%REPO%" :launcher:checkRetailPreserved "-Pretail.stock=%SROOT%" "-Pretail.staged=%STAGE%" --offline -q || (
  echo ERROR: the staged data changes retail content; see the [retail-check] lines above
  exit /b 1
)
set "MOD=%STAGE%\data\mods\monmmo-lost-knights.zip"
if not exist "%MOD%" set "MOD=-"

echo Patching %APK% (server keys, config, data, strings, mod)...
"%JDK%\java.exe" -Xmx2g "%~dp0ApkPackager.java" prepare "%APK%" "%WORK%\unsigned.apk" "%HOST%" "%MOD%" "%REPO%\launcher\src\main\resources\game.public.pem" "%REPO%\launcher\src\main\resources\chat.public.pem" "%STAGE%\data\data.pak" "%STAGE%\data\strings\strings_en.xml" || exit /b 1

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
