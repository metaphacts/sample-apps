#!/bin/bash
#set -e

echo Loading Postgres Dvdrental data
cd /postgres-setup

FILE=/postgres-setup/dvdrental.tar
if [ ! -f "$FILE" ]; then
    echo "Dowloading $FILE from the postgres tutorial."
    wget https://neon.tech/postgresqltutorial/dvdrental.zip
    unzip dvdrental.zip
    rm dvdrental.zip
fi

pg_restore -U postgres -d dvdrental /postgres-setup/dvdrental.tar