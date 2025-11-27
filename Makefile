.PHONY: help build-backend build-frontend test-backend test-frontend run-backend run-frontend dev check-coverage deploy-prod

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
run-backend: ## Run Backend locally in development mode
	@echo "🚀 Starting Backend..."
	cd backend && ./mvnw spring-boot:run

run-frontend: ## Run Frontend locally in development mode
	@echo "🚀 Starting Frontend..."
	cd frontend && npm run dev

dev: ## Start both backend and frontend (run in separate terminals)
	@echo "💡 Tip: Run these commands in separate terminals:"
	@echo "  Terminal 1: make run-backend"
	@echo "  Terminal 2: make run-frontend"

# --- 🛡️ GATES & OPS ---
check-coverage: ## Run Tests & Verify 80% Coverage
	@echo "🔍 Checking Backend Coverage..."
	cd backend && ./mvnw verify
	@echo "🔍 Checking Frontend Coverage..."
	cd frontend && npm run test:coverage

deploy-prod: ## Deploy to Production
	fly deploy --config fly.prod.toml
