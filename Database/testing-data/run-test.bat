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
    pause
    exit /b 1
)

echo Resetting data...
mysqlsh --user root --password %password% --host %host% --file "resetData.sql"
echo Sending vehicle #1...
start cmd /C mysqlsh --user root --password %password% --host %host% --file "test-1.sql"
timeout /t 30
echo Sending vehicle #2...
start cmd /C mysqlsh --user root --password %password% --host %host% --file "test-2.sql"
timeout /t 30
echo Sending vehicle #3...
start cmd /C mysqlsh --user root --password %password% --host %host% --file "test-3.sql"
timeout /t 30
echo Sending vehicle #4...
start cmd /C mysqlsh --user root --password %password% --host %host% --file "test-4.sql"
timeout /t 30
echo Sending vehicle #5...
start cmd /C mysqlsh --user root --password %password% --host %host% --file "test-5.sql"

echo Done
pause