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

## Build Commands

**Always use Makefile targets** (never raw `mvn` or `npm` commands):

```bash
make build-backend       # Build Spring Boot project
make build-frontend      # Build Next.js project
make test-backend        # Run backend tests
make test-frontend       # Run frontend tests
make check-coverage      # Verify 80% coverage (REQUIRED before PR)
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
- **Auth:** Clerk manages users externally (no password storage)

## When Working on Issues

1. Check if backend or frontend task (scope your context accordingly)
2. Read relevant section of PRD.md
3. Write failing tests first
4. Implement to make tests pass
5. Run `make check-coverage` before committing
