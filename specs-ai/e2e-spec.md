# TraderPlanner E2E Spec (Playwright + TypeScript)

Goal:
Create a minimal, senior-quality E2E suite for TraderPlanner (MVP) using TypeScript + Playwright.
Focus only on 2 flows:
1) Trading CRUD (create -> edit -> delete) + verify GET /api/trading returns 200 and empty array.
2) Portfolio CRUD (create -> edit -> delete) + verify GET /api/portfolio returns 200 and empty array.

Constraints:
- Keep it MVP: no extra features, no huge abstractions.
- Reliability over beauty: stable locators, auto-waits, clean assertions.
- Use Playwright test runner.
- No Page Object overengineering. Use small helpers if needed.
- Tests must be deterministic and isolated: always clean up created data.

Assumptions:
- Frontend runs at http://localhost:5173
- Backend runs at http://localhost:8080
- Swagger exists but not needed.
- UI has routes:
  - /trading
  - /portfolio

Hard requirements:
- Use Playwright "request" fixture for API verification:
  - After UI delete, call GET http://localhost:8080/api/trading -> expect 200 and [].
  - After UI delete, call GET http://localhost:8080/api/portfolio -> expect 200 and [].
- Prefer role-based locators, data-testid if present, otherwise stable text/labels.
- If current UI lacks stable selectors, add data-testid attributes in frontend minimally (only what's needed).

Test 1: Trading CRUD
Steps:
- Navigate to /trading
- Create new trading row (ticker must exist in whitelist, use AAPL by default)
- Verify row appears
- Edit the row (change note, optionally entry price if present/optional)
- Verify row updated
- Delete the row
- Verify table no longer contains that row
- API check: GET /api/trading returns 200 and empty array

Test 2: Portfolio CRUD
Steps:
- Navigate to /portfolio
- Create new portfolio position (use ticker AAPL, quantity 1, buy price 100, target price 120, notes)
- Verify row appears
- Edit the row (change notes and quantity)
- Verify row updated
- Delete the row
- Verify row removed
- API check: GET /api/portfolio returns 200 and empty array

Project structure:
- e2e/
  - tests/
    - trading-crud.spec.ts
    - portfolio-crud.spec.ts
  - playwright.config.ts
  - package.json
  - README.md

Playwright config:
- baseURL default: http://localhost:5173
- trace: on-first-retry
- video: retain-on-failure
- screenshot: only-on-failure
- retries: 1 (local), 2 (CI optional)
- workers: 1 by default to avoid data collisions

Run commands:
From repo root:
- cd e2e
- npm install
- npm run test

Add docs:
- e2e/README.md must explain prerequisites and how to run.