# TraderPlanner E2E Framework (Playwright + TypeScript)

E2E suite for Trading and Portfolio: POM, API layer, fixtures, test data builders, negative cases. **Selector policy: only `page.getByTestId(...)`** (and `request` for API). No text/content locators. No arbitrary waits.

## Prerequisites

- **Backend** at `http://localhost:8080`
- **Frontend** at `http://localhost:5173`
- **PostgreSQL** (e.g. `docker compose up -d`)
- **Node.js 18+**

## Commands (Windows PowerShell)

From repo root:

```powershell
cd e2e
npm install
npx playwright install
npm run test
```

If you want to see how tests are running in the browser then
cd C:\workprojects\traderpanner-app\e2e
npx playwright test --headed


- **UI mode:** `npm run test:ui`
- **Report:** `npm run report` (after a run)

## Unique note/notes and prefix-only cleanup

All created entities use a **crypto-safe unique marker** in the note/notes field (e.g. `E2E|trading|w0|<uuid>`) so tests can resolve the row id via the API list and avoid UI text locators.

- **Trading:** prefix format `E2E|trading|w<workerIndex>|` + UUID.
- **Portfolio:** prefix format `E2E|portfolio|w<workerIndex>|` + UUID.

Cleanup is **prefix-only**: tests delete only rows whose note/notes **start with** their worker prefix, then verify none remain with that prefix. The DB can contain other data; tests do not require an empty DB.

## Parallel safety

Tests are safe to run in parallel. Each worker uses its own prefix (`E2E|trading|w0|`, `E2E|portfolio|w1|`, etc.), so workers do not collide. Config uses `workers: process.env.CI ? 2 : undefined` (default workers when not in CI).

## Negative cases and validation

Negative tests cover mandatory fields per backend DTOs. Each negative case uses a unique note/notes (with the same worker prefix). After submitting invalid data, tests assert: validation error visible, no new entry/position with that note/notes via API, and list count unchanged.

## Structure

- `src/api` – apiClient, tradingApi (findIdByNote, deleteE2ETradingAndVerify, assertNoEntriesWithPrefix), portfolioApi (findIdByNotes, deleteE2EPortfolioAndVerify, assertNoPositionsWithPrefix), types
- `src/pages` – TradingPage, PortfolioPage (testid-only; expectValidationErrorVisible / expectValidationErrorNotVisible)
- `src/fixtures` – baseFixture
- `src/data` – builders (crypto-safe unique note/notes, optional prefix), datasets (negative cases as function of prefix)
- `src/utils` – assertions, crudHelpers (create+resolve with poll timeout diagnostics)
- `tests/trading` – trading-crud.spec.ts, trading-negative.spec.ts
- `tests/portfolio` – portfolio-crud.spec.ts, portfolio-negative.spec.ts

Config: workers from env (CI ? 2 : default), baseURL `http://localhost:5173`, backend `http://localhost:8080`.
