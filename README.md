# AI Expense Assistant

Personal expense tracking with a React UI and a Spring Boot backend, plus a conversational AI that calls the same expense and analytics services as the manual UI.

Design docs: [docs/PROJECT_OVERVIEW.md](docs/PROJECT_OVERVIEW.md) · Implementation stages: [docs/IMPLEMENTATION_DESIGN.md](docs/IMPLEMENTATION_DESIGN.md) · UI mockups: [ui-mockups/](ui-mockups/)

## Planned versions (assumption A1)

| Component | Version | Notes |
|-----------|---------|--------|
| Java | 21 | LTS |
| Spring Boot | **3.4.6** | Pinned in `backend/pom.xml`; [start.spring.io](https://start.spring.io) currently offers Boot 4.x only — we stay on 3.4.x until Spring AI compatibility for Boot 4 is verified for this project |
| Spring AI | **1.1.0** | BOM in `backend/pom.xml`; Google GenAI starter (Gemini Developer API) |
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

### Gemini / Spring AI (Stage 5A)

1. Create a free [Gemini Developer API key](https://aistudio.google.com/apikey).
2. In `.env`, set `GEMINI_API_KEY` and `SPRING_AI_MODEL_CHAT=google-genai` (without both, the backend still starts but AI beans stay off).
3. With the backend running under the `dev` profile and `GEMINI_API_KEY` set, the assistant chat API is available at `POST /api/v1/assistant/chat` (see Stage 5C below). If `SPRING_AI_MODEL_CHAT` is unset, the backend still starts but assistant beans stay off.

   **Free tier (default model `gemini-3.5-flash-lite`):** about 15 requests/min and 500 requests/day per Google Cloud project (shared across all API keys in that project). Live limits: [AI Studio → Rate limits](https://aistudio.google.com/rate-limit). Do not send real customer PII on the free tier (Google may use prompts for model improvement). Gemini 3.5+ models do not use custom `temperature` in `application.yml`; output length is capped via `max-output-tokens`. For stronger reasoning experiments only, you can switch to `gemini-3.8-flash` in `application.yml` (much lower quota: ~5 RPM / 20 RPD).

### System prompt (Stage 5B)

Prompt template: `backend/src/main/resources/prompts/system.st`. Behaviour is exercised through the chat endpoint (Stage 5C).

### Assistant chat (Stage 5C)

Load env vars before starting (`set -a && source .env && set +a` from repo root). Chat persists to MySQL (`conversations`, `chat_messages`); LLM context uses `SPRING_AI_CHAT_MEMORY` (last 20 messages). History APIs: `GET /api/v1/conversations` and related routes.

```bash
curl -s -X POST http://localhost:8080/api/v1/assistant/chat \
  -H 'Content-Type: application/json' \
  -d '{"message":"What is today'\''s date?"}'
```

Expect `{"reply":"..."}`. Empty `message` → **400** `VALIDATION_ERROR`. Missing/invalid API key or provider errors → **503** `AI_UNAVAILABLE`. Optional header `X-Dev-User-Id` selects the dev user (same as other APIs until Stage 10).

Try also: `"What did I spend this month?"` — the assistant should call **`getTotalSpending`** and match your analytics API (Stage 7A). Manual expense APIs keep working when the LLM is down.

### Tool calling – total spending (Stage 7A)

Chat can call **`getTotalSpending`** (no `userId` in the tool schema; the server passes it via `ToolContext`). Compare with REST:

```bash
curl -s "http://localhost:8080/api/v1/analytics/total?from=2026-11-01&to=2026-11-25"

curl -s -X POST http://localhost:8080/api/v1/assistant/chat \
  -H 'Content-Type: application/json' \
  -d '{"message":"How much did I spend this month?"}'
```

The reply total should match `totalAmount` for the same date range and user. Use `X-Dev-User-Id: 2` to verify isolation. Tool call logging: `com.expenseassistant.assistant.tools` at DEBUG in the `dev` profile.

### Analytics tools (Stage 7B)

Chat also exposes **`getCategorySpending`**, **`getHighestSpendingCategory`**, **`getMonthlySummary`**, and **`getDateRangeReport`**. Compare chat answers with the matching REST endpoints for the same user and dates:

| Chat example | REST |
|---|---|
| How much on food this month? | `GET /api/v1/analytics/by-category?from=…&to=…&category=FOOD` |
| Which category is highest? | `GET /api/v1/analytics/highest-category?from=…&to=…` |
| Compare October and November | Two `GET /api/v1/analytics/monthly?year=…&month=…` calls |
| Spending report for a range | `GET /api/v1/analytics/report?from=…&to=…` |

```bash
curl -s -X POST http://localhost:8080/api/v1/assistant/chat \
  -H 'Content-Type: application/json' \
  -d '{"message":"How much did I spend on food this month?"}'
```

### Structured extraction (Stage 6A–6B)

Debug/learning endpoint (does **not** save to the database). Returns `draft`, `missing` field names, and a clarification `question`:

```bash
curl -s -X POST http://localhost:8080/api/v1/assistant/extract \
  -H 'Content-Type: application/json' \
  -d '{"message":"I spent ₹500 on dinner yesterday"}'
```

Examples to try manually: `"bus 40 rupees last Friday"`, `"coffee"`, `"spent 500"`. Extraction prompt: `backend/src/main/resources/prompts/extraction.st`. Stage 7 moves add-by-chat to the `addExpense` tool; this route may be removed in Stage 11.

### Expense tools and clarification (Stage 7C–7E)

Chat exposes **`searchExpenses`** and **`addExpense`** (same data paths as REST). Compare list/create with:

```bash
curl -s "http://localhost:8080/api/v1/expenses?from=2026-11-24&to=2026-11-25&size=20"

curl -s -X POST http://localhost:8080/api/v1/assistant/chat \
  -H 'Content-Type: application/json' \
  -d '{"message":"What did I spend yesterday?"}'
```

| Chat example | Expected behaviour |
|---|---|
| What did I spend yesterday? | `searchExpenses` with concrete dates; real rows or honest empty result |
| Show food expenses this week | `searchExpenses` with category=FOOD and week range |
| I spent ₹500 on dinner yesterday | `addExpense` → row in DB and expense list UI |
| Same complete add message twice | Two rows (documented duplicate behaviour) |

**Clarification (7E)** — try these without expecting a DB row until details are clear:

- `"Add dinner"` → assistant should ask amount/date; **no** expense row yet.
- Follow up `"₹450 on food today"` → one row after `addExpense`.
- `"spent 500 on stuff"` → category question before create.

### Delete and edit by chat (Stage 7G–7H)

- **`proposeExpenseDeletion(expenseId)`** — loads the expense, creates a pending action; the row stays until you confirm.
- **`proposeExpenseUpdate(expenseId, …)`** — partial fields merged with the current expense; validated like REST `PUT`.
- Chat response may include `pendingAction`: `{ actionId, type, summary, expiresAt }`.
- **`POST /api/v1/assistant/actions/{actionId}/confirm`** — applies delete/update (single use).
- **`POST /api/v1/assistant/actions/{actionId}/cancel`** — discards the proposal.

TTL default **10** minutes (`app.pending-action.ttl-minutes`). Typing “yes” in chat does **not** confirm — only the confirm endpoint does.

**Postman sketch:** create an expense → chat “delete expense {id}” (or search then propose) → copy `pendingAction.actionId` → `POST …/confirm` with dev user → expense gone; `cancel` leaves it unchanged.

6. Tests (MySQL must be running, `DB_*` and `SPRING_PROFILES_ACTIVE=dev` set):

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