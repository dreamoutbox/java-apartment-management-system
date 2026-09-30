#!/bin/sh
set -e

# Stop running server if port 8080 is held to safely release SQLite file locks
if command -v lsof >/dev/null 2>&1 && lsof -ti:8080 >/dev/null 2>&1; then
    echo "Stopping process listening on port 8080..."
    if command -v fuser >/dev/null 2>&1; then
        fuser -k 8080/tcp >/dev/null 2>&1 || true
    else
        kill $(lsof -ti:8080) >/dev/null 2>&1 || true
    fi
    sleep 1
fi

echo "Removing data directory..."
rm -rf ./data
mkdir -p ./data/uploads

echo "Data reset complete. Database and uploads cleared."
