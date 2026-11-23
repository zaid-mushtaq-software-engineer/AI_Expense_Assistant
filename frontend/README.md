# AI Expense Assistant — Frontend (Stage 4)

React + TypeScript + Vite SPA for manual expense and analytics UI (no auth; dev backend supplies user via `dev` profile).

## Setup

```bash
cd frontend
cp .env.example .env.development   # if needed
npm install
```

### Environment

| Variable | Example | Purpose |
|----------|---------|---------|
| `VITE_API_BASE_URL` | `http://localhost:8080/api/v1` | REST client base path |
| `VITE_DEV_USER_ID` | `1` (optional) | Sends `X-Dev-User-Id` for two-user isolation tests |

Actuator health checks use the same host with `/api/v1` stripped.

## Run

```bash
npm run dev    # default http://localhost:5173
npm run build
```

Backend must run with `spring.profiles.active=dev` and CORS allowing the Vite origin.