#!/bin/sh
set -e

if command -v lsof >/dev/null 2>&1 && lsof -ti:8080 >/dev/null 2>&1; then
    echo "App is running on port 8080. Seeding mock data via API..."
    RESPONSE=$(curl -s -w "\n%{http_code}" -X POST http://localhost:8080/api/dev/mock-data)
    BODY=$(echo "$RESPONSE" | sed '$d')
    STATUS=$(echo "$RESPONSE" | tail -n1)
    if [ "$STATUS" = "200" ]; then
        echo "$BODY"
    else
        echo "Failed to seed mock data. HTTP status: $STATUS"
        echo "$BODY"
        exit 1
    fi
else
    echo "App is not running. Bootstrapping mock data seeder..."
    ./mvnw spring-boot:run -Dspring-boot.run.arguments="--mock-data" -Dspring-boot.run.profiles=dev -q
fi
