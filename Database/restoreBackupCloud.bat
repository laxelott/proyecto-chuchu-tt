@echo off

echo Reading credentials...
for /f "delims=" %%A in (../password) do (
    set "password=%%A"
)
for /f "delims=" %%A in (../host) do (
    set "host=%%A"
)
if not defined password if not defined host (
    echo Error: Credential files not found!
    exit /b 1
)

echo Running...
echo NO
@REM mysqlsh --user root --password %password% --host %host% --file chuchu_database.sql
@REM mysqlsh --user root --password %password% --host %host% --file chuchu_sproc.sql
@REM mysqlsh --user root --password %password% --host %host% --file chuchu_data.sql