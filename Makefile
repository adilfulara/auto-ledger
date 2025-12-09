.PHONY: help build-backend build-frontend test-backend test-frontend dev-db-start dev-db-stop dev-start dev-stop check-coverage

# Default target - show help
.DEFAULT_GOAL := help

help: ## Show this help message
	@echo "Available commands:"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-20s\033[0m %s\n", $$1, $$2}'

# --- ⚡ FAST SUB-PROJECT COMMANDS ---
build-backend: ## Build only the Java Backend
	@echo "☕ Building Backend..."
	cd backend && ./mvnw clean package -DskipTests

build-frontend: ## Build only the Next.js Frontend
	@echo "⚛️ Building Frontend..."
	cd frontend && npm run build

test-backend: ## Run Backend Unit Tests
	cd backend && ./mvnw test

test-frontend: ## Run Frontend Unit Tests
	cd frontend && npm run test

# --- 🚀 LOCAL DEVELOPMENT ---
dev-db-start: ## Start PostgreSQL with sample data
	@echo "🐳 Starting PostgreSQL database..."
	@docker-compose -f docker-compose.dev.yml up -d
	@echo "⏳ Waiting for database to be ready..."
	@until docker exec auto-ledger-postgres pg_isready -U postgres > /dev/null 2>&1; do sleep 1; done
	@echo "✅ PostgreSQL ready at localhost:5432"

dev-db-stop: ## Stop PostgreSQL
	@echo "🛑 Stopping PostgreSQL..."
	@docker-compose -f docker-compose.dev.yml down

dev-start: dev-db-start ## Start DB + Backend with local profile
	@echo "🚀 Starting Backend (local profile with sample data)..."
	@echo "💡 Database: jdbc:postgresql://localhost:5432/autoledger_dev"
	cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=local

dev-stop: ## Stop backend and database
	@echo "🛑 Stopping Backend..."
	@lsof -ti:9090 | xargs kill -9 2>/dev/null || echo "No backend process on port 9090"
	@echo "🛑 Stopping PostgreSQL..."
	@docker-compose -f docker-compose.dev.yml down
	@echo "✅ Environment stopped"

# --- 🛡️ GATES & OPS ---
check-coverage: ## Run Tests & Verify 80% Coverage
	@echo "🔍 Checking Backend Coverage..."
	cd backend && ./mvnw verify
	@echo "🔍 Checking Frontend Coverage..."
	cd frontend && npm run test:coverage
