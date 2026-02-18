# TraderPlanner E2E Framework (Playwright + TypeScript)

E2E suite for Trading and Portfolio: POM, API layer, fixtures, test data builders, negative cases. **Selector policy: only `page.getByTestId(...)`** (and `request` for API). No text/content locators. No arbitrary waits.

## Prerequisites & install

- **Node.js 18+**
- **Backend** and **frontend** must be running before tests (see [Preflight](#preflight-readiness-check) below).
- **PostgreSQL** (e.g. `docker compose up -d` from repo root).

From repo root (Windows PowerShell):

```powershell
cd e2e
npm install
npx playwright install
```

## How to run tests locally

Run all tests (frontend and backend must be up; preflight will fail fast with a clear message if not):

```powershell
cd e2e
npx playwright test
```

Or from repo root:

```powershell
cd e2e; npx playwright test
```

- **Headed mode (see browser):** `npx playwright test --headed`
- **UI mode:** `npm run test:ui`
- **Filter by test name:** `npx playwright test --grep "Portfolio"` (or `--grep "Trading"`, etc.)

## Environment variables

E2E URLs are **env-based** so you can override defaults (e.g. different ports or CI).

| Variable | Purpose | Default |
|----------|---------|---------|
| `E2E_BASE_URL` or `PLAYWRIGHT_BASE_URL` | Frontend base URL (Playwright `baseURL`) | `http://localhost:5173` |
| `E2E_API_BASE_URL` or `API_BASE_URL` | Backend API base URL (used by e2e API client) | `http://localhost:8080` |

- **playwright.config.ts** uses `getBaseURL()` from `src/config/env.ts` for `baseURL`.
- The e2e **API client** uses `getApiBaseURL()` for all `/api/*` requests.

Example (PowerShell) if your frontend runs on a different port:

```powershell
$env:E2E_BASE_URL = "http://localhost:3000"
npx playwright test
```

## Preflight (readiness check)

Before any test runs, **globalSetup** (`globalSetup.ts`) runs a readiness check so tests fail fast with a clear error instead of many `ECONNREFUSED` timeouts.

- **Playwright** runs `globalSetup: './globalSetup.ts'` once before the test run.
- **Frontend:** GET `baseURL` until 2xx (retry loop with timeout). If it never responds, you get an error telling you to start the frontend and how to set `E2E_BASE_URL` / `PLAYWRIGHT_BASE_URL`.
- **Backend:** Tries endpoints in order and stops on **first success**:
  1. `GET {apiBase}/actuator/health` — must return 200 and body containing `"status":"UP"`.
  2. `GET {apiBase}/swagger-ui/index.html` — 200.
  3. `GET {apiBase}/api/portfolio` — 200.
  4. `GET {apiBase}/api/trading` — 200.

If **all** backend endpoints fail, the error lists each URL and the last error seen (e.g. connection refused, status code), plus hints: start the backend (Spring Boot), set `E2E_API_BASE_URL` if needed, and check the terminal/logs where the backend is running. **No log parsing** — the message only points you to where to look.

**Typical failure:** You see "Preflight failed: Frontend" or "Preflight failed: Backend unreachable" with `ECONNREFUSED` or a status code. **Fix:** Start the frontend and backend (see root README "Local run"), ensure ports match the defaults or the env vars above, and check the terminal where each app runs.

## Parallel safety & prefix-only cleanup

Tests are **parallel-safe** and do **not** require an empty database.

- Each created entity uses a **crypto-safe unique marker** in the note/notes field (e.g. `E2E|trading|w0|<uuid>`) so tests can resolve the row id via the API and avoid UI text locators.
- **Trading:** prefix format `E2E|trading|w<workerIndex>|` + UUID.
- **Portfolio:** prefix format `E2E|portfolio|w<workerIndex>|` + UUID.

**Cleanup is prefix-only:** tests delete only rows whose note/notes **start with** their worker prefix, then verify that no rows with that prefix remain. They never delete all data. The DB can contain other (non-E2E) data; tests will not wipe it. In CI, multiple workers may run (e.g. `workers: 2`); each uses its own prefix, so there are no cross-test collisions.

## Reports

After a run, the HTML report is under `e2e/playwright-report/`. To open it:

```powershell
cd e2e
npm run report
```

## Negative cases and validation

Negative tests cover mandatory fields per backend DTOs. Each negative case uses a unique note/notes (with the same worker prefix). After submitting invalid data, tests assert: validation error visible, no new entry/position with that note/notes via API, and list count unchanged (via prefix-count helper).

## Structure

- `src/config` — `env.ts` (getBaseURL, getApiBaseURL)
- `src/api` — apiClient, tradingApi, portfolioApi, types
- `src/pages` — TradingPage, PortfolioPage (testid-only; expectValidationErrorVisible / expectValidationErrorNotVisible)
- `src/fixtures` — baseFixture
- `src/data` — builders (crypto-safe unique note/notes, optional prefix), datasets (negative cases)
- `src/utils` — preflight (waitForHttpOk), prefixCount, assertions, crudHelpers
- `globalSetup.ts` — preflight: frontend + backend readiness (uses preflight.ts)
- `tests/trading` — trading-crud.spec.ts, trading-negative.spec.ts
- `tests/portfolio` — portfolio-crud.spec.ts, portfolio-negative.spec.ts
