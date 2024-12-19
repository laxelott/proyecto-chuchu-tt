@echo off

echo Reading credentials...
for /f "delims=" %%A in (password) do (
    set "password=%%A"
)
for /f "delims=" %%A in (host) do (
    set "host=%%A"
)
if not defined password if not defined host (
    echo Error: Credential files not found!
    pause
    exit /b 1
)

echo Running...
mysqlsh --user root --password %password% --host %host% --file "chuchu_sproc.sql"
echo Done
pause