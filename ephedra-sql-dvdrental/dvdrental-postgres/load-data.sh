#!/bin/bash
#set -e

echo Loading Postgres Dvdrental data
cd /postgres-setup

FILE=/postgres-setup/dvdrental.tar
if [ ! -f "$FILE" ]; then
    echo "Dowloading $FILE from the postgres tutorial."
    wget http://www.postgresqltutorial.com/wp-content/uploads/2019/05/dvdrental.zip
    unzip dvdrental.zip
    rm dvdrental.zip
fi

pg_restore -U postgres -d dvdrental /postgres-setup/dvdrental.tar