#!/bin/sh
set -e

COMPOSE_FILE="docker-compose.demo.yml"

HOST_PORT="${PORT:-8080}"

echo "Starting Apartment Management System (Demo) on port ${HOST_PORT}..."
PORT="${HOST_PORT}" docker compose -f "${COMPOSE_FILE}" up -d "$@"

echo "Container status:"
docker compose -f "${COMPOSE_FILE}" ps

echo ""
echo "App is accessible at: http://localhost:${HOST_PORT}"
echo "H2/SQLite Console:    http://localhost:${HOST_PORT}/h2-console"
echo "Default accounts:"
echo "  - Landlord:   landlord / password"
echo "  - Manager:    manager / password"
echo "  - Accountant: accountant / password"
echo "  - Technician: technician / password"
echo "  - Tenants:    tenant1 .. tenant20 / password"
