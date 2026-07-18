#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

cd "$PROJECT_ROOT"

echo "╔═══════════════════════════════════════════════╗"
echo "║   OOP Integration Test Runner                 ║"
echo "╚═══════════════════════════════════════════════╝"
echo ""

cleanup() {
  echo ""
  echo "Cleaning up..."
  docker compose down -v 2>/dev/null || true
}
trap cleanup EXIT

echo "Step 1: Building services..."
mvn clean package -DskipTests -q

echo "Step 2: Starting Docker Compose stack..."
docker compose up -d --build

echo "Step 3: Waiting for services to be healthy (max 4 min)..."
MAX_WAIT=48
for i in $(seq 1 $MAX_WAIT); do
  ALL_UP=true
  for port in 8080 8081 8082 8083 8084 8085 8087; do
    curl -sf "http://localhost:$port/actuator/health" > /dev/null 2>&1 || ALL_UP=false
  done
  if [ "$ALL_UP" = "true" ]; then
    echo ""
    echo "All services are healthy!"
    break
  fi
  if [ "$i" -eq "$MAX_WAIT" ]; then
    echo ""
    echo "ERROR: Services did not become healthy in time."
    echo "Service status:"
    for port in 8080 8081 8082 8083 8084 8085 8087; do
      STATUS=$(curl -sf "http://localhost:$port/actuator/health" 2>/dev/null | grep -o '"status":"[^"]*"' || echo "DOWN")
      echo "  Port $port: $STATUS"
    done
    docker compose logs --tail=50
    exit 1
  fi
  printf "."
  sleep 5
done

echo ""
echo "Step 4: Running integration tests..."
mvn verify -pl test/integration -Pintegration \
  -Dgateway.url=http://localhost:8080 \
  -Dkeycloak.url=http://localhost:8180

echo ""
echo "╔═══════════════════════════════════════════════╗"
echo "║   Integration tests passed!                   ║"
echo "╚═══════════════════════════════════════════════╝"
