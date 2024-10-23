@echo off
python combineFiles.py

pause
mysqlsh --user root --password --host localhost --file chuchu_sproc.sql