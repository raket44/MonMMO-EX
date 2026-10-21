@echo off
rem Manages login accounts on the VPS (over SSH with the pirated_vps key).
rem   vps-user.cmd set <username> <password>     create the account or change its password
rem   vps-user.cmd delete <username>             remove the account
rem   vps-user.cmd list                          show all accounts
setlocal
set "VPS="
rem The server address lives in ..\local\server.env (outside the repo), never in git.
if exist "%~dp0..\local\server.env" for /f "usebackq tokens=1,* delims==" %%A in ("%~dp0..\local\server.env") do if "%%A"=="PIRATED_VPS" set "VPS=%%B"
if "%VPS%"=="" echo Put PIRATED_VPS=user@host in ..\local\server.env first.& exit /b 1
set "KEY=%USERPROFILE%\.ssh\pirated_vps"
set "PSQL=cd /opt/pirated && . ./.env && docker exec -i login-db psql -U $LOGIN_DB_USER -d $LOGIN_DB_NAME -v ON_ERROR_STOP=1"
set "SQL=%TEMP%\pirated-user.sql"
if /i "%~1"=="list" (
  echo select username, created_at from users order by id;> "%SQL%"
  goto run
)
if /i "%~1"=="delete" (
  if "%~2"=="" goto usage
  echo delete from users where username=lower('%~2'^);> "%SQL%"
  goto run
)
if /i not "%~1"=="set" goto usage
if "%~3"=="" goto usage
for /f "usebackq" %%H in (`powershell -NoProfile -Command "$sha=[Security.Cryptography.SHA1]::Create(); ($sha.ComputeHash([Text.Encoding]::UTF8.GetBytes('%~3')) | ForEach-Object { $_.ToString('x2') }) -join ''"`) do set "HASH=%%H"
echo insert into users (username, display_name, password_hash^) values (lower('%~2'^), '%~2', '%HASH%'^) on conflict (username^) do update set password_hash = excluded.password_hash;> "%SQL%"
:run
ssh -i "%KEY%" %VPS% "%PSQL%" < "%SQL%"
if errorlevel 1 (
  echo Could not update the accounts on the server.
  del "%SQL%"
  exit /b 1
)
del "%SQL%"
if /i "%~1"=="set" echo Account '%~2' is ready on the Pirated Version server.
exit /b
:usage
echo usage: vps-user.cmd set ^<username^> ^<password^>  ^|  delete ^<username^>  ^|  list
exit /b 2
