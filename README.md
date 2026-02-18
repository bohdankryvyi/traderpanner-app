# TraderPlanner — App (MVP)

Monorepo:
- `app-backend` — Spring Boot REST API (Java 25, Spring Boot 3.4.7)
- `app-frontend` — React 18 + TypeScript + Vite + Tailwind
- `e2e` — Playwright + TypeScript E2E tests (separate module inside this repo)

> E2E test implementation details and the full runbook are documented in `e2e/README.md`.

> AI recommendation feature is in development and will be fully completed in the next iterations.
> Unit and API tests are in development and will be fully completed in the next iterations.

## What is this app?

TraderPlanner is a small demo app with 4 pages:
- Home (`/`) — description + disclaimer + navigation
- Trading (`/trading`) — CRUD table for current trades + AI button "Analyze now"
- Portfolio (`/portfolio`) — Excel-like CRUD table for long-term portfolio analytics
- Help (`/help`) — static help/disclaimer page

## Tech stack (MVP)

Backend:
- Java 25 (Temurin)
- Spring Boot 3.4.7
- Spring Web, Validation, Spring Data JPA
- Flyway
- PostgreSQL 16
- springdoc-openapi 2.x (Swagger UI)
- Spring Boot Actuator
- SLF4J/Logback

Frontend:
- React 18 + TypeScript + Vite + Tailwind CSS
- React Router, Axios
- i18n EN/UA

Infra (local):
- Docker Compose (Postgres)

## Prerequisites

- Java 25
- Node.js (recommended: Node 20 LTS; newer is usually OK)
- Git
- Docker Desktop + WSL2

## Local run

### One-command run (Postgres + backend + frontend + E2E)

From the repo root you can start everything, run E2E tests, and then stop services in one go:

```powershell
powershell -ExecutionPolicy Bypass -File .\run-e2e.ps1
```

The script sets `JAVA_TOOL_OPTIONS="-Duser.timezone=UTC"` for the backend automatically, so no manual timezone step is needed. To stop backend, frontend, and Docker Compose (e.g. if you started them manually or the script was interrupted):

```powershell
powershell -ExecutionPolicy Bypass -File .\stop-local.ps1
```

### Manual run

1. Start PostgreSQL:
```bash
docker compose up -d
```

2. Run backend:
```bash
cd app-backend
.\mvnw.cmd spring-boot:run
```
Note: Run $env:JAVA_TOOL_OPTIONS="-Duser.timezone=UTC"
If you receive a timezone issue on a build failure. Then retry .\mvnw.cmd spring-boot:run again

Backend: `http://localhost:8080`  
Swagger UI: `http://localhost:8080/swagger-ui/index.html`

3. Run frontend:
```bash
cd app-frontend
npm install
npm run dev
```
Frontend: `http://localhost:5173`

On **PowerShell**, for custom logging quote the `-D` expression:  
`.\mvnw.cmd "-Dspring-boot.run.arguments=--logging.level.com.traderplanner=DEBUG" spring-boot:run`

## E2E tests (Playwright)

E2E tests live in the **`e2e/`** module and run against a running backend and frontend. They cover:

- **Trading:** CRUD (create → edit → delete) and negative validation; API verification that no E2E-prefixed entries remain after cleanup.
- **Portfolio:** CRUD and negative validation; API verification that no E2E-prefixed positions remain after cleanup.

Tests use prefix-only cleanup (no delete-all) and are parallel-safe; the DB does not need to be empty.

**Run locally (PowerShell):**

```powershell
cd e2e
npm install
npx playwright install
npm run test
```

Start the backend and frontend first (see [Local run](#local-run) above). A preflight check runs before tests and fails fast with a clear message if frontend or backend is unreachable. For full details (env vars, preflight, reports), see **[e2e/README.md](e2e/README.md)**.

## CI: GitHub Actions E2E

- **Workflow file:** [.github/workflows/e2e.yml](.github/workflows/e2e.yml)
- **When it runs:** On every pull request and on manual trigger (`workflow_dispatch`).
- **What CI starts:**
  - **Postgres** — service container `postgres:16` (port 5432, healthcheck).
  - **Backend** — Spring Boot on `:8080` (DB env vars point at Postgres; `JAVA_TOOL_OPTIONS=-Duser.timezone=UTC`). AI keys are empty in CI; E2E does not test AI and the pattern endpoint returns 200 with a fallback.
  - **Frontend** — `npm ci` + `npm run build` in `app-frontend`, then `npm run preview -- --host 127.0.0.1 --port 5173` with `VITE_API_BASE_URL=http://localhost:8080`.
  - **Playwright** — runs from the `e2e/` directory after preflight passes.
- **Artifacts (uploaded even on failure):**
  - **playwright-report** — HTML report.
  - **app-logs** — `backend.log`, `frontend.log` when present.
- **Branch protection:** To require E2E before merge, add the **e2e** job as a required status check in branch protection rules.

## How the app works (including AI)

- **Frontend ↔ Backend:** The frontend calls the backend at `/api/*`. Base URL is set by `VITE_API_BASE_URL` (default `http://localhost:8080`).

- **Prices:** `GET /api/prices?tickers=...` returns USD prices. When `ALPHAVANTAGE_API_KEY` is configured, the backend can use Alpha Vantage for quotes; otherwise it falls back to mock/deterministic prices.

- **Trading page:** CRUD for trading entries (ticker, note, entry price). Entry price is optional. Currency has been removed from Trading. Current price per row comes from the prices API.

- **Portfolio page:** CRUD for long-term positions. Backend computes and returns current price, upside %, invested amount, share %, expected profit, etc. All calculations in USD.

- **AI (Analyze now):**
  - Endpoint: `POST /api/ai/pattern?tf=1h|1d`
  - **Always returns 200.** If `OPENAI_API_KEY` is missing or invalid, or rate limit exceeded, or OpenAI errors, the backend returns a deterministic fallback result (same JSON shape with `source` indicating fallback). The app keeps working without keys.
  - Response: `timeframe`, `ticker`, `pattern`, `rationale`, `generatedAt`, `source`. `source` is one of: `openai`, `openai-no-market-data`, `fallback`, `fallback-rate-limit`, `fallback-openai-error`, `fallback-openai-invalid`.
  - Results are cached per timeframe for `ai.cacheTtlMinutes`. Cache hits do not consume the daily request limit.
  - Optional: `ALPHAVANTAGE_API_KEY` for OHLC data; when missing or rate-limited, OpenAI is still called with reduced context and `source` may be `openai-no-market-data`.

## Environment variables

**Secrets:** Set all keys and passwords via environment variables or a local `.env` file. Never commit `.env` or paste real keys into the repo or docs.

**Backend** (from environment only):
- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` — PostgreSQL connection (defaults in application.yml if unset)
- `OPENAI_API_KEY` — used for AI pattern analysis when set; if missing, endpoint returns 200 with fallback (no error)
- `ALPHAVANTAGE_API_KEY` — optional; used for OHLC/market data in pattern analysis and for price quotes (missing → mock prices / pattern without candles)

**Frontend:**
- `VITE_API_BASE_URL` — backend base URL (default `http://localhost:8080`)

Example files: repo root `.env.example` (DB + backend), `app-frontend/.env.example` (frontend). Copy these to `.env` and set values locally; **do not commit `.env` or real API keys.**

## Running AI locally and on CI

- **Local (with OpenAI):** Set `OPENAI_API_KEY` via environment (or a local `.env` that is **not** committed). Optionally set `ALPHAVANTAGE_API_KEY` for candle data.
- **Windows PowerShell — per session (use your own key; never commit it):**
  ```powershell
  $env:OPENAI_API_KEY = "<your-openai-key>"
  $env:ALPHAVANTAGE_API_KEY = "<optional-alpha-vantage-key>"
  cd app-backend
  .\mvnw.cmd spring-boot:run
  ```
- **Windows PowerShell — persist for current user (optional):** Use `[System.Environment]::SetEnvironmentVariable("OPENAI_API_KEY", "<your-key>", "User")` and re-read in new terminals with `$env:OPENAI_API_KEY = [System.Environment]::GetEnvironmentVariable("OPENAI_API_KEY","User")`. Never commit keys.
- **CI (GitHub Actions):** E2E workflow leaves `OPENAI_API_KEY` and `ALPHAVANTAGE_API_KEY` empty. The backend must not fail; the pattern endpoint returns 200 with a fallback result, so tests are unaffected.

**Backend AI config** (`application.yml` / `ai.*`):
- `ai.enabled` — default `true`; when false, pattern still returns fallback
- `ai.cacheTtlMinutes` — cache TTL for pattern (and tips)
- `ai.maxRequestsPerDay` — simple in-memory daily cap on actual OpenAI calls (default 10); exceeding returns fallback
- `ai.model` — OpenAI model (default `gpt-4o-mini`)

Free-tier limits and the daily limiter: OpenAI and Alpha Vantage have rate/cost limits; the app limits how many times per day it calls OpenAI (`ai.maxRequestsPerDay`) to stay within a small budget.

## Test pattern endpoint (curl)

Backend running (keys optional; no keys → fallback 200).

```bash
curl -X POST "http://localhost:8080/api/ai/pattern?tf=1d" -H "Content-Type: application/json"
```

Example 200 with OpenAI:
```json
{"timeframe":"1d","ticker":"AAPL","pattern":"Ascending Triangle","rationale":"Price has formed higher lows...","generatedAt":"2025-02-06T12:00:00Z","source":"openai"}
```

Example 200 fallback (no key or rate limit):
```json
{"timeframe":"1d","ticker":"MSFT","pattern":"Bull Flag","rationale":"Deterministic fallback selection (no AI)...","generatedAt":"2025-02-06T12:00:00Z","source":"fallback"}
```

```bash
curl -X POST "http://localhost:8080/api/ai/pattern?tf=1h" -H "Content-Type: application/json"
```

**Alpha Vantage:** When configured, the app fetches OHLC for a few tickers per analyze (candidates rotate by UTC day). Without it, pattern analysis still runs using the ticker whitelist only.
