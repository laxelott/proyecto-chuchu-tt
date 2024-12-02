@echo off
echo Running...
mysqlsh --user root --password --host localhost --file chuchu_sproc.sql
echo Done
pause