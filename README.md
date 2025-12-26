# Auto Ledger

[![CI](https://github.com/adilfulara/auto-ledger/actions/workflows/ci.yml/badge.svg)](https://github.com/adilfulara/auto-ledger/actions/workflows/ci.yml)
[![Java](https://img.shields.io/badge/Java-21%20(Corretto)-orange?logo=amazon-aws)](https://aws.amazon.com/corretto/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.0-brightgreen?logo=spring)](https://spring.io/projects/spring-boot)

A production-grade car mileage tracking application that serves both humans (via Next.js web interface) and AI agents (via Model Context Protocol server).

## Features

- 🚗 Track fuel fill-ups and calculate MPG
- 📊 Historical trend analysis
- 🤖 AI-agent accessible via MCP
- 🔒 Secure authentication with Clerk
- 📱 Responsive web interface
- ☁️ Scale-to-zero architecture (~$5/mo hosting)

## Tech Stack

### Backend
- **Language:** Java 21 (Amazon Corretto)
- **Framework:** Spring Boot 3.4.0
- **Database:** PostgreSQL
- **AI Integration:** Spring AI (MCP Server)
- **Testing:** JUnit 5, Mockito, Testcontainers
- **Coverage:** 80% minimum (JaCoCo)

### Frontend
- **Framework:** Next.js 14+ (App Router)
- **Styling:** Tailwind CSS + Shadcn/UI
- **State Management:** React Query (TanStack Query)
- **Testing:** Jest, React Testing Library

### Infrastructure
- **Platform:** Fly.io
- **Domain/CDN:** Cloudflare
- **CI/CD:** GitHub Actions
- **Monitoring:** Sentry + Better Stack

## Quick Start

### Prerequisites

1. **Install SDKMAN** (for Java version management):
```bash
curl -s "https://get.sdkman.io" | bash
```

2. **Install Java 21 (Amazon Corretto)**:
```bash
sdk install java 21-amzn
sdk default java 21-amzn
```

3. **Auto-switch Java version** (when entering project directory):
```bash
sdk env install
```

### Build & Test

```bash
# Backend
make build-backend       # Build Spring Boot project
make test-backend        # Run tests
make check-coverage      # Verify 80% coverage gate

# Frontend (coming soon - issue #4)
make build-frontend
make test-frontend
```

### Local Development

```bash
# Start backend (in one terminal)
make run-backend

# Start frontend (in another terminal)
make run-frontend
```

## Development Workflow

This project follows a **TDD + GitHub Issues** workflow:

1. All work tracked via GitHub Issues
2. Create feature branches: `feat/issue-{number}-description`
3. Write tests BEFORE implementation (TDD)
4. Verify 80% coverage gate passes
5. Create Pull Request for review
6. CI/CD handles deployment

**CRITICAL:** Never commit directly to `main`. Always use feature branches and PRs.

### Branch Protection Rules

The `main` branch has the following protection rules enforced:

| Rule | Setting | Purpose |
|------|---------|---------|
| **Require PR** | ✅ Enabled | All changes must go through a Pull Request |
| **Required status checks** | `Backend Build & Test` | CI must pass before merge |
| **Strict status checks** | ✅ Enabled | Branch must be up-to-date with main |
| **Dismiss stale reviews** | ✅ Enabled | Re-review required after new commits |
| **Force pushes** | ❌ Blocked | History cannot be rewritten |
| **Branch deletion** | ❌ Blocked | Main branch cannot be deleted |

### Merge Settings (Clean History)

| Setting | Value | Purpose |
|---------|-------|---------|
| **Squash merge** | ✅ Only allowed | Single commit per PR for clean history |
| **Merge commits** | ❌ Disabled | Prevents merge commit clutter |
| **Rebase merge** | ❌ Disabled | Squash is the only option |
| **Commit title** | PR Title | Uses PR title as commit message |
| **Commit message** | All commits | Includes all PR commit messages |
| **Auto-delete branches** | ✅ Enabled | Cleans up merged feature branches |

## Project Structure

```
auto-ledger/
├── backend/            # Spring Boot 3 backend
│   ├── src/main/
│   ├── src/test/
│   └── pom.xml
├── frontend/           # Next.js 14 frontend
│   ├── src/app/
│   └── package.json
├── .github/workflows/  # CI/CD pipelines
├── Makefile           # Build orchestration
└── CLAUDE.md          # AI assistant guidelines
```

## Test Coverage

Backend test coverage is enforced at **80% minimum** for both line and branch coverage.

**View Coverage Reports:**
- Coverage summary is displayed in the [GitHub Actions workflow summary](https://github.com/adilfulara/auto-ledger/actions/workflows/ci.yml)
- Detailed HTML reports are available as artifacts in each CI run
- Download the `jacoco-report` artifact from any CI run to view detailed coverage

## Code Reviews

This project supports AI-assisted code reviews via Claude:

**On-Demand Reviews:**
1. **Manual trigger via @claude**: Comment `@claude please review this PR` on any PR or issue
2. **GitHub Actions UI**: Go to Actions → Claude Code Review → Run workflow → Enter PR number

**Note:** Automatic reviews on every commit are disabled to save usage. Use manual triggers when you need a review.

## Contributing

See [CLAUDE.md](./CLAUDE.md) for development guidelines and context management rules.

## License

MIT
