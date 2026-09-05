@echo off
rem Adds a login account to the running login database (the login-db Docker container).
rem   add-user.cmd <username> <password>
rem The client sends the unsalted SHA-1 of the password, so that is what gets stored.
setlocal
if "%~2"=="" (
  echo usage: add-user.cmd ^<username^> ^<password^>
  exit /b 2
)
set "USERNAME_LC=%~1"
for /f "usebackq eol=# tokens=1,* delims==" %%A in ("%~dp0.env") do if /i "%%A"=="LOGIN_DB_NAME" set "DBNAME=%%B"
for /f "usebackq eol=# tokens=1,* delims==" %%A in ("%~dp0.env") do if /i "%%A"=="LOGIN_DB_USER" set "DBUSER=%%B"
for /f "usebackq" %%H in (`powershell -NoProfile -Command "$sha=[Security.Cryptography.SHA1]::Create(); ($sha.ComputeHash([Text.Encoding]::UTF8.GetBytes('%~2')) | ForEach-Object { $_.ToString('x2') }) -join ''"`) do set "HASH=%%H"
docker exec login-db psql -U %DBUSER% -d %DBNAME% -v ON_ERROR_STOP=1 -c "INSERT INTO users (username, display_name, password_hash) VALUES (lower('%USERNAME_LC%'), '%USERNAME_LC%', '%HASH%') ON CONFLICT (username) DO UPDATE SET password_hash = EXCLUDED.password_hash;"
if errorlevel 1 (
  echo Could not add the user. Is Docker and the login-db container running?
  exit /b 1
)
echo Account '%USERNAME_LC%' is ready. Use it on the MonMMO-EX login screen.
