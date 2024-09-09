import platform
import sys
import subprocess

target_docker = ""

# Combine files
filenames = [
    "chuchu_database.sql",
    "chuchu_sproc.sql",
    "chuchu_data.sql"
]
with open('chuchu.sql', 'w') as f_out:
    for file in filenames:
        with open(file) as f_in:
            for line in f_in:
                f_out.write(line)
        f_out.write("\n\n\n\n")
