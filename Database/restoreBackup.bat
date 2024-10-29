@echo off
echo Running...
mysqlsh --user root --password --host localhost --file chuchu_database.sql
mysqlsh --user root --password --host localhost --file chuchu_sproc.sql
mysqlsh --user root --password --host localhost --file chuchu_data.sql