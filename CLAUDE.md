# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Required Reading

**ALWAYS read `PRD.md` first** - It contains the complete project architecture, data models, tech stack, and implementation requirements.

## Development Workflow

This project uses a **TDD + GitHub Issues workflow**:

1. All work is tracked via GitHub Issues
2. Create feature branches: `feat/issue-{number}-description`
3. Write tests BEFORE implementation (TDD)
4. Verify 80% coverage gate passes: `make check-coverage`
5. Create Pull Request for review
6. CI/CD handles deployment based on branch/tag

**CRITICAL: NEVER commit directly to `main` branch.** Always use feature branches and PRs. The repository is configured for squash and merge to maintain a clean history.

### Branch Naming Convention

- **Feature**: `feat/issue-{number}-description` (e.g., `feat/issue-33-staging-cicd`)
- **Bug fix**: `fix/issue-{number}-description` (e.g., `fix/issue-48-use-fly-registry`)
- **Documentation**: `docs/issue-{number}-description` (e.g., `docs/issue-50-update-deployment-docs`)

### Workflow Applies to ALL Changes

Documentation updates, configuration changes, and code changes ALL require:
1. GitHub Issue
2. Feature branch
3. Pull Request

No exceptions - even for "quick documentation fixes".

## Local Development Environment

**Java Version Management:**

This project uses **Java 21 (Amazon Corretto)**. Use SDKMAN to manage Java versions:

```bash
# Install SDKMAN (if not already installed)
curl -s "https://get.sdkman.io" | bash

# Install Java 21 (Amazon Corretto)
sdk install java 21-amzn

# Set as default
sdk default java 21-amzn

# Verify version
java -version  # Should show "Corretto-21.x.x"
```

**IMPORTANT:** Always use SDKMAN for Java version management to ensure consistency across environments.

## Local Development Setup

### Quick Start

```bash
# Start PostgreSQL + Backend with sample data
make dev-start

# Backend runs at: http://localhost:9090
# PostgreSQL at: localhost:5432
```

### Spring Profiles

The project uses Spring profiles to manage different environments:

- **`local`** (default for `make dev-start`) - Loads sample data via Flyway migrations
  - 2 users: Alice (alice@example.com), Bob (bob@example.com)
  - 3 cars: Tesla Model 3, Honda Civic, Ford F-150
  - 36 fillups with realistic MPG patterns

- **Default** (no profile) - Schema only, no sample data

### Testing the API

Use IntelliJ HTTP Client with the provided request files:

```bash
backend/http/cars.http      # 19 requests for Cars API
backend/http/fillups.http   # 23 requests for Fillups API
```

Open these files in IntelliJ and click the ▶️ icon next to each request.

### Sample Data UUIDs

For manual testing, these UUIDs are available:

```
Users:
  Alice: a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11
  Bob:   b1ffcd88-8b1a-5df7-aa5c-5aa8ac271b22

Cars:
  Tesla Model 3:  c2ddef77-7a2b-6ef8-99fc-499b9d482c33 (Alice)
  Honda Civic:    d3eeff66-6b3c-7df9-88eb-388a8c593d44 (Alice)
  Ford F-150:     e4fff055-5c4d-8ef0-77da-277b7d604e55 (Bob)
```

### Database Access

```bash
# Query the database directly
docker exec auto-ledger-postgres psql -U postgres -d autoledger_dev

# Example queries
docker exec auto-ledger-postgres psql -U postgres -d autoledger_dev -c "SELECT * FROM users;"
docker exec auto-ledger-postgres psql -U postgres -d autoledger_dev -c "SELECT * FROM cars;"
docker exec auto-ledger-postgres psql -U postgres -d autoledger_dev -c "SELECT COUNT(*) FROM fillups;"
```

### Resetting the Database

```bash
# Stop everything and remove data volume
make dev-stop
docker volume rm auto-ledger_autoledger_dev_data

# Start fresh
make dev-start
```

## Build Commands

**Always use Makefile targets** (never raw `mvn` or `npm` commands):

```bash
# Development
make dev-start           # Start PostgreSQL + Backend (local profile with sample data)
make dev-stop            # Stop backend and database
make dev-db-start        # Start only PostgreSQL
make dev-db-stop         # Stop only PostgreSQL

# Building
make build-backend       # Build Spring Boot project
make build-frontend      # Build Next.js project

# Testing
make test-backend        # Run backend tests
make test-frontend       # Run frontend tests
make check-coverage      # Verify 80% coverage (REQUIRED before PR)

# Authentication (Staging)
make auth-enable-staging ARGS="--dry-run"  # Preview Clerk auth setup
make auth-enable-staging                   # Enable Clerk auth in staging
make auth-disable-staging                  # Disable auth (use test user)
make auth-test-staging ARGS="--jwt $TOKEN" # Test auth with JWT
```

## Context Management

This is a **monorepo** with isolated sub-projects:

- **Backend tasks:** Only read/edit `./backend` directory
- **Frontend tasks:** Only read/edit `./frontend` directory

Do not cross-pollinate to save context window.

## Critical Testing Requirements

- **Coverage Gate:** 80% minimum (enforced by JaCoCo + Jest)
- **MPG Calculation:** Must test all 3 cases (normal, partial fill, missed fill) - see PRD.md Section 5
- **Build must fail** if coverage < 80%

## Key Implementation Notes

- **CDS Optimization:** Already configured in `backend/Dockerfile` for <2s startup
- **MCP Server:** Backend exposes AI agent interface via SSE (see PRD.md Section 6)
- **Database:** Single Postgres cluster with 3 logical DBs (dev/staging/prod)
- **Auth:** Provider-agnostic JWT validation (Clerk recommended, no SDK required)

## Authentication Documentation

- **Setup Guide:** `docs/CLERK-SETUP.md` - Comprehensive Clerk setup for staging/production
- **Staging Auth:** `STAGING-AUTH.md` - Current state and quick reference
- **Scripts:** `scripts/clerk/setup.sh` and `scripts/clerk/test.sh`
- **Testing:** `scripts/clerk/testing/` - Developer helpers for API testing
- **Architecture:** Backend validates JWTs using JWKS (see `backend/src/main/java/me/adilfulara/autoledger/auth/`)

## When Working on Issues

1. Check if backend or frontend task (scope your context accordingly)
2. Read relevant section of PRD.md
3. Write failing tests first
4. Implement to make tests pass
5. Run `make check-coverage` before committing
