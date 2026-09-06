@echo off
rem Manages login accounts on the VPS (over SSH with the monmmo_vps key).
rem   vps-user.cmd set <username> <password>     create the account or change its password
rem   vps-user.cmd delete <username>             remove the account
rem   vps-user.cmd list                          show all accounts
setlocal
set "VPS=root@91.98.41.154"
set "KEY=%USERPROFILE%\.ssh\monmmo_vps"
set "PSQL=cd /opt/monmmo && . ./.env && docker exec -i login-db psql -U $LOGIN_DB_USER -d $LOGIN_DB_NAME -v ON_ERROR_STOP=1"
set "SQL=%TEMP%\monmmo-user.sql"
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
if /i "%~1"=="set" echo Account '%~2' is ready on the MonMMO-EX server.
exit /b
:usage
echo usage: vps-user.cmd set ^<username^> ^<password^>  ^|  delete ^<username^>  ^|  list
exit /b 2
