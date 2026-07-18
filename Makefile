.PHONY: help build up down logs test clean status verify wait-healthy

help: ## Show this help
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-20s\033[0m %s\n", $$1, $$2}'

# ============================================================
# BUILD
# ============================================================

build: build-backend build-frontend ## Build all services

build-backend: ## Build backend services
	mvn clean package -DskipTests

build-frontend: ## Build frontend apps
	cd apps && pnpm install && pnpm build

# ============================================================
# DOCKER COMPOSE
# ============================================================

up: ## Start all services (Docker Compose)
	docker compose up -d
	@echo ""
	@echo "  Platform starting..."
	@echo "  ─────────────────────────────────────"
	@echo "  Gateway:    http://localhost:8080"
	@echo "  Operate:    http://localhost:3000"
	@echo "  Tasklist:   http://localhost:3001"
	@echo "  Analytics:  http://localhost:3002"
	@echo "  Modeler:    http://localhost:3003"
	@echo "  Keycloak:   http://localhost:8180"
	@echo "  Kafka:      localhost:29092"
	@echo "  PostgreSQL: localhost:5432"
	@echo "  ─────────────────────────────────────"

up-infra: ## Start only infrastructure (DB, Kafka, Keycloak, Redis)
	docker compose up -d postgres kafka keycloak redis

down: ## Stop all services
	docker compose down

down-clean: ## Stop all services and remove volumes
	docker compose down -v

logs: ## Tail logs for all services
	docker compose logs -f

logs-engine: ## Tail logs for runtime bundle
	docker compose logs -f runtime-bundle

# ============================================================
# DEVELOPMENT
# ============================================================

dev-backend: ## Run backend services locally (requires infra running)
	@echo "Start services individually with: mvn spring-boot:run -pl platform/<service>"

dev-operate: ## Run Operate UI in dev mode
	cd apps && pnpm --filter @oop/operate dev

dev-tasklist: ## Run Tasklist UI in dev mode
	cd apps && pnpm --filter @oop/tasklist dev

dev-analytics: ## Run Analytics UI in dev mode
	cd apps && pnpm --filter @oop/analytics dev

dev-modeler: ## Run Modeler UI in dev mode
	cd apps && pnpm --filter @oop/modeler dev

# ============================================================
# TESTING
# ============================================================

test: test-backend test-frontend ## Run all tests

test-backend: ## Run backend tests
	mvn test

test-frontend: ## Run frontend tests
	cd apps && pnpm test

test-integration: ## Run integration tests (requires Docker)
	mvn verify -pl test/integration -am -Pintegration

test-e2e: ## Run E2E tests (requires running stack)
	cd test/e2e && pnpm playwright test

load-test: ## Run load tests
	k6 run test/load/scenarios/basic-process.js

# ============================================================
# UTILITIES
# ============================================================

format: ## Format all code
	mvn spotless:apply
	cd apps && pnpm format

format-check: ## Check code formatting
	mvn spotless:check
	cd apps && pnpm format:check

status: ## Check service health
	@echo "Checking services..."
	@printf "  Gateway:        " && (curl -sf http://localhost:8080/actuator/health | jq -r '.status' 2>/dev/null || echo "DOWN")
	@printf "  Runtime Bundle: " && (curl -sf http://localhost:8081/actuator/health | jq -r '.status' 2>/dev/null || echo "DOWN")
	@printf "  Query Service:  " && (curl -sf http://localhost:8082/actuator/health | jq -r '.status' 2>/dev/null || echo "DOWN")
	@printf "  Audit Service:  " && (curl -sf http://localhost:8083/actuator/health | jq -r '.status' 2>/dev/null || echo "DOWN")
	@printf "  Analytics:      " && (curl -sf http://localhost:8084/actuator/health | jq -r '.status' 2>/dev/null || echo "DOWN")
	@printf "  Notifications:  " && (curl -sf http://localhost:8085/actuator/health | jq -r '.status' 2>/dev/null || echo "DOWN")
	@printf "  Identity:       " && (curl -sf http://localhost:8087/actuator/health | jq -r '.status' 2>/dev/null || echo "DOWN")
	@printf "  Keycloak:       " && (curl -sf http://localhost:8180/health/ready 2>/dev/null && echo "UP" || echo "DOWN")
	@printf "  PostgreSQL:     " && (docker compose exec -T postgres pg_isready -U oop > /dev/null 2>&1 && echo "UP" || echo "DOWN")
	@printf "  Kafka:          " && (docker compose exec -T kafka kafka-broker-api-versions.sh --bootstrap-server localhost:9092 > /dev/null 2>&1 && echo "UP" || echo "DOWN")
	@printf "  Redis:          " && (docker compose exec -T redis redis-cli ping > /dev/null 2>&1 && echo "UP" || echo "DOWN")

verify: up ## Full stack verification (start + health + integration test)
	@echo "Waiting for all services to be healthy..."
	@sleep 5
	@$(MAKE) status
	@echo ""
	@echo "Running integration tests..."
	mvn verify -pl test/integration -Pintegration

wait-healthy: ## Wait until all services are healthy (max 3 min)
	@echo "Waiting for services to start..."
	@for i in $$(seq 1 36); do \
		ALL_UP=true; \
		for port in 8080 8081 8082 8083 8084 8085 8087; do \
			curl -sf http://localhost:$$port/actuator/health > /dev/null 2>&1 || ALL_UP=false; \
		done; \
		if [ "$$ALL_UP" = "true" ]; then echo "All services healthy!"; exit 0; fi; \
		printf "."; sleep 5; \
	done; \
	echo ""; echo "ERROR: Services did not become healthy in time"; exit 1

deploy-sample: ## Deploy sample process
	curl -X POST http://localhost:8080/api/v1/processes \
		-H "Authorization: Bearer $$(make _get-token)" \
		-F "file=@samples/order-process.bpmn"

_get-token:
	@curl -s -X POST http://localhost:8180/realms/oop/protocol/openid-connect/token \
		-d "client_id=backend-service" \
		-d "client_secret=backend-service-secret" \
		-d "username=admin" \
		-d "password=admin" \
		-d "grant_type=password" | jq -r .access_token

clean: ## Clean build artifacts
	mvn clean
	cd apps && pnpm -r exec rm -rf dist node_modules
