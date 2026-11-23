# AI Expense Assistant

Personal expense tracking with a React UI and a Spring Boot backend, plus a conversational AI that calls the same expense and analytics services as the manual UI.

Design docs: [docs/PROJECT_OVERVIEW.md](docs/PROJECT_OVERVIEW.md) · Implementation stages: [docs/IMPLEMENTATION_DESIGN.md](docs/IMPLEMENTATION_DESIGN.md) · UI mockups: [ui-mockups/](ui-mockups/)

## Planned versions (assumption A1)

| Component | Version | Notes |
|-----------|---------|--------|
| Java | 21 | LTS |
| Spring Boot | **3.4.6** | Pinned in `backend/pom.xml`; [start.spring.io](https://start.spring.io) currently offers Boot 4.x only — we stay on 3.4.x until Spring AI compatibility for Boot 4 is verified for this project |
| Spring AI | **1.1.x** (planned) | Added in Stage 5; verify BOM against Boot 3.4.x at that stage |
| MySQL | 8.4 | Docker image in `docker-compose.yml` |

## Prerequisites

- JDK 21
- Maven 3.9+
- Docker and Docker Compose
- Node.js 20+ and npm (frontend)

## Database (Stage 0B)

The app uses **MySQL 8.4** with database `expense_assistant`, charset `utf8mb4`, collation `utf8mb4_0900_ai_ci`. See [docs/DATABASE_DESIGN.md](docs/DATABASE_DESIGN.md).

**Recommended (Docker):** `docker compose up -d` — creates the database and user `expense_app` (never use `root` from the application). Credentials match [.env.example](.env.example) (`DB_PASSWORD=change_me` by default).

**Native install (optional):** create the same database and user, then set `DB_URL`, `DB_USER`, and `DB_PASSWORD` in `.env`.

**Schema management:** `spring.jpa.hibernate.ddl-auto=validate` — Hibernate does not create or alter tables. All schema changes come from **Flyway** migrations (`V1` `users`, `V2` `expenses`, …). See [docs/DATABASE_DESIGN.md](docs/DATABASE_DESIGN.md).

### Dev identity (Stage 1C — remove in Stage 10D)

Until JWT auth exists, the `dev` profile uses `DevCurrentUserProvider` so services can take a real `userId`:

- Default user: `app.dev-user-id` (default **1**), seeded as `dev-user-a@example.com` / password `password`.
- Second test user: id **2** (`dev-user-b@example.com`, same password).
- Override in Postman or curl with header **`X-Dev-User-Id: 2`** to exercise isolation between users.
- Postman: import [`postman/AI_Expense_Assistant.postman_collection.json`](postman/AI_Expense_Assistant.postman_collection.json) and [`postman/Local_Dev.postman_environment.json`](postman/Local_Dev.postman_environment.json) (Stage 2H).

Seed data is applied only from `classpath:db/migration/dev` (enabled in `application-dev.yml`, not in production profile config). The app **refuses to start** if both `dev` and `prod` profiles are active while the dev provider is registered.

### 0B verification checklist

| Check | How | Expected |
|-------|-----|----------|
| MySQL running | `docker compose ps` | Service `healthy` |
| App user | `DB_USER=expense_app` in `.env` | Not `root` |
| Charset | `docker exec expense-assistant-mysql mysql -uexpense_app -pchange_me -e "SHOW CREATE DATABASE expense_assistant"` | `utf8mb4_0900_ai_ci` |
| Flyway | Backend startup log | Migrations applied (V1+) |
| DB health | `curl -s http://localhost:8080/actuator/health` | `"db":{"status":"UP"}` |
| Tests | `cd backend && mvn test` (with `.env` loaded) | PASS |

Flyway may warn that MySQL 8.4 is newer than its tested range; safe to ignore for local development.

## Quick start

1. Start MySQL:

   ```bash
   docker compose up -d
   ```

2. Configure environment (credentials must match `docker-compose.yml`):

   ```bash
   cp .env.example .env
   # edit .env if needed; default DB_PASSWORD=change_me matches compose
   ```

3. Run the backend (from repo root, export vars or use your shell’s dotenv support):

   ```bash
   set -a && source .env && set +a
   cd backend && mvn spring-boot:run
   ```

4. Check health:

   ```bash
   curl -s http://localhost:8080/actuator/health
   ```

5. Tests (MySQL must be running, `DB_*` and `SPRING_PROFILES_ACTIVE=dev` set):

   ```bash
   set -a && source .env && set +a
   cd backend && mvn test
   ```

## Frontend (Stage 0C)

1. Ensure the backend is running (steps above) with the `dev` profile (CORS allows Vite on port 5173).

2. Install and start the UI:

   ```bash
   cd frontend
   npm install
   npm run dev
   ```

3. Open [http://localhost:5173](http://localhost:5173). You should see **Backend status: UP** and **Database: UP** with no CORS errors in the browser console.

API base URL is set in `frontend/.env.development` as `VITE_API_BASE_URL=http://localhost:8080` (see `frontend/.env.example`).

**CORS check (optional):**

```bash
curl -s -D - -o /dev/null -H "Origin: http://localhost:5173" http://localhost:8080/actuator/health | grep -i access-control
```

Expect `Access-Control-Allow-Origin: http://localhost:5173`.

Installed for later stages: React Router, TanStack Query, React Hook Form, Zod, Recharts.

## Repository layout

| Path | Purpose |
|------|---------|
| `backend/` | Spring Boot API |
| `frontend/` | React app (Stage 0C) |
| `docs/` | Architecture and API design |
| `ui-mockups/` | Static HTML screen references |

## Environment variables

See [.env.example](.env.example). Never commit `.env`.