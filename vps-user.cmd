@echo off
rem Manages login accounts on the VPS (over SSH with the monmmo_vps key).
rem   vps-user.cmd set <username> <password>     create the account or change its password
rem   vps-user.cmd delete <username>             remove the account
rem   vps-user.cmd list                          show all accounts
setlocal
set "VPS=root@91.98.41.154"
set "KEY=%USERPROFILE%\.ssh\monmmo_vps"
if /i "%~1"=="list" (
  ssh -i "%KEY%" %VPS% "cd /opt/monmmo && . ./.env && docker exec login-db psql -U $LOGIN_DB_USER -d $LOGIN_DB_NAME -Atc \"select username, created_at from users order by id\""
  exit /b
)
if /i "%~1"=="delete" (
  if "%~2"=="" goto usage
  ssh -i "%KEY%" %VPS% "cd /opt/monmmo && . ./.env && docker exec login-db psql -U $LOGIN_DB_USER -d $LOGIN_DB_NAME -c \"delete from users where username=lower('%~2')\""
  exit /b
)
if /i not "%~1"=="set" goto usage
if "%~3"=="" goto usage
for /f "usebackq" %%H in (`powershell -NoProfile -Command "$sha=[Security.Cryptography.SHA1]::Create(); ($sha.ComputeHash([Text.Encoding]::UTF8.GetBytes('%~3')) | ForEach-Object { $_.ToString('x2') }) -join ''"`) do set "HASH=%%H"
ssh -i "%KEY%" %VPS% "cd /opt/monmmo && . ./.env && docker exec login-db psql -U $LOGIN_DB_USER -d $LOGIN_DB_NAME -c \"insert into users (username, display_name, password_hash) values (lower('%~2'), '%~2', '%HASH%') on conflict (username) do update set password_hash = excluded.password_hash\""
if errorlevel 1 (
  echo Could not reach the server.
  exit /b 1
)
echo Account '%~2' is ready on the MonMMO-EX server.
exit /b
:usage
echo usage: vps-user.cmd set ^<username^> ^<password^>  ^|  delete ^<username^>  ^|  list
exit /b 2
