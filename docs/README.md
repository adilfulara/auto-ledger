# Auto Ledger Documentation

Welcome to the Auto Ledger documentation. This directory contains all project documentation organized by topic.

## Quick Start

- **[Project Overview](../README.md)** - Start here for project introduction and tech stack
- **[Product Requirements](PRD.md)** - Complete system architecture and requirements document

## Guides

### Authentication

- **[Clerk Setup Guide](auth/CLERK-SETUP.md)** - Comprehensive Clerk authentication setup
  - Step-by-step account and application creation
  - JWT template configuration
  - Getting test tokens
  - Troubleshooting guide

- **[Staging Authentication](auth/STAGING-AUTH.md)** - Quick reference for staging environment
  - Current auth state
  - How to enable/disable auth
  - Testing without authentication

### Development

- **[HTTP Testing Guide](development/HTTP-TESTING.md)** - Testing APIs with IntelliJ HTTP Client
  - Using cars.http and fillups.http request files
  - Prerequisites and setup
  - Available test cases

- **[Scripts Reference](../scripts/README.md)** - Infrastructure automation scripts
  - Clerk authentication management
  - Fly.io application setup
  - Supabase database management

## For Claude Code

See [CLAUDE.md](../CLAUDE.md) for development workflow instructions, context management, and testing requirements.

## Documentation Structure

```
docs/
├── README.md              # This file (documentation index)
├── PRD.md                 # Product Requirements Document
├── auth/                  # Authentication documentation
│   ├── CLERK-SETUP.md     # Comprehensive Clerk setup guide
│   └── STAGING-AUTH.md    # Staging environment quick reference
└── development/           # Developer resources
    └── HTTP-TESTING.md    # API testing guide
```

## Related Resources

- **[GitHub Issues](https://github.com/adilfulara/auto-ledger/issues)** - Track bugs and features
- **[Pull Requests](https://github.com/adilfulara/auto-ledger/pulls)** - Code reviews
- **[CI/CD Actions](https://github.com/adilfulara/auto-ledger/actions)** - Build and test status
