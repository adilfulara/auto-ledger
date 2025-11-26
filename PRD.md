# 🛠️ AI Agent Prompt: Car Mileage Tracker (Production Edition)

## 1. 🎯 Objective
Build a production-grade, low-cost **Car Mileage Tracking Application** ("MileageMate") that serves two masters:
1.  **Humans:** Via a responsive **Next.js** web interface.
2.  **AI Agents:** Via a compliant **Model Context Protocol (MCP)** server.

**Core Philosophy:** "Scale to Zero." The system prioritizes low cost (~$5/mo) by sleeping when idle, while enforcing strict quality gates (80% test coverage) and deep observability.

---

## 2. 🏗️ Project Structure & Context Strategy

**CRITICAL INSTRUCTION FOR AI AGENTS:**
This project is divided into two distinct sub-projects to maintain context efficiency. Do not cross-pollinate dependencies.

### 2.1 Directory Layout
```text
/mileage-mate
├── CLAUDE.md           # Master context rules (This file)
├── Makefile            # Unified orchestration commands
├── backend/            # [Sub-Project] Java Spring Boot 3
│   ├── src/
│   ├── pom.xml         # Maven dependencies
│   └── Dockerfile      # CDS-optimized build
└── frontend/           # [Sub-Project] Next.js 14
    ├── src/app/
    ├── package.json
    └── Dockerfile      # Standalone build
```

### 2.2 Context Rules
* **Backend Tasks:** Only read/edit files in `./backend`. Ignore `./frontend` to save context.
* **Frontend Tasks:** Only read/edit files in `./frontend`. Ignore `./backend`.
* **Commands:** ALWAYS use the `Makefile` targets (e.g., `make test-backend`) instead of raw `mvn` or `npm` commands to ensure consistency.

---

## 3. ⚙️ Architecture & Tech Stack

### Backend (Spring Boot 3.x / Java 21)
-   **Framework:** Spring Boot 3 (Web, Data JPA, Security, Validation).
-   **AI Integration:** Spring AI (MCP Server).
-   **Optimization:** **Class Data Sharing (CDS)** enabled for <2s startup time (critical for serverless).
-   **Testing:** JUnit 5, Mockito, Testcontainers.

### Frontend (Next.js 14+)
-   **Framework:** Next.js (App Router).
-   **Styling:** Tailwind CSS + Shadcn/UI.
-   **State:** React Query (TanStack Query).
-   **Testing:** Jest, React Testing Library.

### Infrastructure (The "Lean" Stack)
-   **Platform:** Fly.io (Compute & DB).
-   **Domain:** Cloudflare (DNS, Edge Cache, WAF).
-   **CI/CD:** GitHub Actions.
-   **Observability:** Sentry (Errors) + Better Stack (Uptime/Logs).

---

## 4. 📊 Quality Assurance Gates

The build **must fail** if these metrics are not met.

### 4.1 Backend Coverage (JaCoCo)
* **Threshold:** **80%** Minimum Line & Branch Coverage.
* **Exclusions:** DTOs, Configuration classes, and generated code (Lombok).
* **Tool:** JaCoCo Maven Plugin (configured in `pom.xml`).

### 4.2 Frontend Coverage (Jest)
* **Threshold:** **80%** Global Coverage (Statements, Branches, Functions, Lines).
* **Tool:** Jest Coverage Thresholds (configured in `jest.config.js`).

---

## 5. 🧱 Data Entities & Schema

### `User`
* Managed externally by Clerk.
* **Fields:** `auth_provider_id` (PK, String), `email` (String), `created_at` (Timestamp).

### `Car`
* **Fields:** `id` (UUID), `user_id` (FK), `make`, `model`, `year`, `vin`, `name`.
* **Settings:** `primary_fuel_unit` (Enum: GALLONS/LITERS), `primary_distance_unit` (Enum: MILES/KM).

### `Fillup`
* **Fields:** `id` (UUID), `car_id` (FK), `date` (Timestamp), `odometer` (Long), `fuel_volume` (Decimal), `price_per_unit` (Decimal), `total_cost` (Decimal).
* **Flags:**
    * `is_partial` (Boolean): User did not fill the tank to the top (Logic: Skip MPG calc).
    * `is_missed` (Boolean): User forgot to log the previous fill-up (Logic: Break MPG chain).

---

## 6. 🤖 MCP Bindings (Agent Interface)

The backend exposes an MCP Server via Server-Sent Events (SSE) for AI clients (Claude Desktop, Cursor).

### 6.1 Resources (Read-Only)
* `mileagemate://garage/cars` - List of all cars owned by the user.
* `mileagemate://cars/{id}/history` - Last 50 fill-up logs for trend analysis.

### 6.2 Tools (Executable Actions)
* `log_fillup`
    * **Arguments:** `car_name` (String), `odometer` (Number), `gallons` (Number), `price` (Number), `is_full` (Boolean).
    * **Logic:** Fuzzy match car name. Validate odometer > last entry.
* `calculate_trip_cost`
    * **Arguments:** `car_name` (String), `distance` (Number).
    * **Logic:** Uses historical MPG to predict fuel cost.

---

## 7. ☁️ Deployment Strategy (Fly.io + Cloudflare)

| Environment | Trigger | Domain | Scale Strategy |
| :--- | :--- | :--- | :--- |
| **Dev** | `feat/*` | `dev.mileage.adilfulara.me` | Auto-stop (0 machines min) |
| **Staging** | `master` | `staging.mileage.adilfulara.me` | Auto-stop (0 machines min) |
| **Prod** | `v*` Tag | `mileage.adilfulara.me` | Always On (1 machine min) |

* **Database:** Single Fly Postgres cluster hosting 3 logical DBs (`mileage_dev`, `mileage_staging`, `mileage_prod`) to save costs.
* **DNS:** Cloudflare Proxied (Orange Cloud) for SSL/TLS and WAF.

---

## 8. 🕵️ Observability & Compliance

### 8.1 Monitoring
* **Sentry:** Catch Java Exceptions and React Error Boundaries (Free Tier).
* **Better Stack:** Monitor `/actuator/health` every 5 mins.
    * *Dev/Staging:* Use "Keyword Monitor" on logs to avoid waking the app unnecessarily.

### 8.2 Legal (Google OAuth Requirements)
* **`/privacy`**: Static page stating email is used only for account identification.
* **`/terms`**: Standard liability disclaimer.
* **`/delete-account`**: GDPR compliance endpoint in User Profile.

---

## 9. 🛠️ Build & Automation (Makefile)

The `Makefile` is the single source of truth.

```makefile
.PHONY: build-backend build-frontend test-backend test-frontend check-coverage deploy-prod

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

# --- 🛡️ GATES & OPS ---
check-coverage: ## Run Tests & Verify 80% Coverage
	@echo "🔍 Checking Backend Coverage..."
	cd backend && ./mvnw verify jacoco:check
	@echo "🔍 Checking Frontend Coverage..."
	cd frontend && npm run test:coverage

deploy-prod: ## Deploy to Production
	fly deploy --config fly.prod.toml
```

---

## 10. 📁 Implementation Configs

### 10.1 Backend `pom.xml` (JaCoCo Gate)
```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.11</version>
    <executions>
        <execution>
            <id>check</id>
            <goals><goal>check</goal></goals>
            <configuration>
                <rules>
                    <rule>
                        <element>BUNDLE</element>
                        <limits>
                            <limit>
                                <counter>LINE</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.80</minimum>
                            </limit>
                        </limits>
                    </rule>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### 10.2 Optimization `Dockerfile` (Spring Boot CDS)
This configuration reduces startup time from ~8s to ~1.5s, enabling "Scale to Zero".

```dockerfile
# Stage 1: Build & Extract Layers
FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /app
COPY . .
RUN ./mvnw clean package -DskipTests
RUN java -Djarmode=tools -jar target/app.jar extract --layers --launcher

# Stage 2: Create CDS Archive
FROM eclipse-temurin:21-jre-jammy AS optimizer
WORKDIR /app
COPY --from=builder /app/app/dependencies/ ./
COPY --from=builder /app/app/spring-boot-loader/ ./
COPY --from=builder /app/app/snapshot-dependencies/ ./
COPY --from=builder /app/app/application/ ./
# Train the JVM to create the Shared Archive
RUN java -XX:ArchiveClassesAtExit=application.jsa -Dspring.context.exit=onRefresh -jar application.jar

# Stage 3: Final Image
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=optimizer /app/ .
ENTRYPOINT ["java", "-XX:SharedArchiveFile=application.jsa", "org.springframework.boot.loader.launch.JarLauncher"]
```

---

## 11. 🚦 Execution Plan

1.  **Initialize:** Create repo structure with `backend/` and `frontend/` folders.
2.  **Infrastructure:** Run `fly launch` to provision the Single DB Cluster.
3.  **Backend Core (TDD):**
    * Create `Fillup` Entity.
    * Write `FillupServiceTest` to cover MPG logic (Normal, Partial, Missed).
    * Pass `check-coverage` gate.
4.  **MCP Layer:** Implement `LogFillupTool` and `GetGarageResource`. Verify with local Inspector.
5.  **Frontend:** Scaffold Next.js, install Shadcn/UI, connect Clerk.
6.  **Observability:** Inject Sentry DSNs.
7.  **Ship:** Configure GitHub Actions with the 3-tier logic and deploy.
