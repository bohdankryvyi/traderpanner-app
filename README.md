# TraderPlanner — App (MVP)

Monorepo:
- `app-backend` — Spring Boot REST API (Java 25, Spring Boot 3.4.7)
- `app-frontend` — React 18 + TypeScript + Vite + Tailwind
- `e2e` — Playwright + TypeScript E2E tests (separate module inside this repo)

> E2E test implementation details and the full runbook are documented in `e2e/README.md`.

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

1. Start PostgreSQL:
```bash
docker compose up -d
```

2. Run backend:
```bash
cd app-backend
.\mvnw.cmd spring-boot:run
```
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

## E2E tests

E2E tests live in the e2e/ module and run against locally running backend + frontend.

```powershell
cd e2e
npm install
npx playwright install
npm run test
```

If you want to see how tests are running in the browser then
cd C:\workprojects\traderpanner-app\e2e
npx playwright test --headed

## How the app works (including AI)

- **Frontend ↔ Backend:** The frontend calls the backend at `/api/*`. Base URL is set by `VITE_API_BASE_URL` (default `http://localhost:8080`).

- **Prices:** `GET /api/prices?tickers=...` returns USD prices. When `ALPHAVANTAGE_API_KEY` is configured, the backend can use Alpha Vantage for quotes; otherwise it falls back to mock/deterministic prices.

- **Trading page:** CRUD for trading entries (ticker, note, entry price). Entry price is optional. Currency has been removed from Trading. Current price per row comes from the prices API.

- **Portfolio page:** CRUD for long-term positions. Backend computes and returns current price, upside %, invested amount, share %, expected profit, etc. All calculations in USD.

- **AI (Analyze now):**
  - Endpoint: `POST /api/ai/pattern?tf=1h|1d`
  - Requires `OPENAI_API_KEY` and `ALPHAVANTAGE_API_KEY`. If `OPENAI_API_KEY` is missing → **400** with message "OPENAI_API_KEY is not configured". If the market data provider is not configured (`ALPHAVANTAGE_API_KEY` missing) or unavailable (e.g. rate limit) → **502** with an informative message.
  - Success (200): JSON with `timeframe`, `ticker`, `pattern`, `rationale`, `generatedAt`.
  - Results are cached per timeframe for `ai.cacheTtlMinutes` to reduce external API calls.

## Environment variables

**Backend** (from environment only):
- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` — PostgreSQL connection (defaults in application.yml if unset)
- `OPENAI_API_KEY` — required for AI pattern endpoint (missing → 400)
- `ALPHAVANTAGE_API_KEY` — required for OHLC/pattern and optional for price quotes (missing/unavailable → 502 for pattern)

**Frontend:**
- `VITE_API_BASE_URL` — backend base URL (default `http://localhost:8080`)

Example files: repo root `.env.example` (DB + backend), `app-frontend/.env.example` (frontend).

## Test pattern endpoint (curl)

Requires backend running with `OPENAI_API_KEY` and `ALPHAVANTAGE_API_KEY` set.

```bash
curl -X POST "http://localhost:8080/api/ai/pattern?tf=1d" -H "Content-Type: application/json"
```

Example 200 response:
```json
{"timeframe":"1d","ticker":"AAPL","pattern":"Ascending Triangle","rationale":"Price has formed higher lows with a flat top...","generatedAt":"2025-02-06T12:00:00Z"}
```

**Alpha Vantage rate limits:** The app fetches OHLC for at most 5 tickers per analyze. Candidates rotate daily (UTC day offset into the sorted whitelist, wrap-around), with a short delay between requests, to stay within the free tier.
