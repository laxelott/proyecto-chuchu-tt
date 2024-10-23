@echo off
python combineFiles.py

mysqlsh --user root --password --host localhost --file chuchu.sql