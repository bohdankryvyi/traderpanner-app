# TraderPlanner MVP — Spec (source of truth)

Single source of truth for current MVP behavior and contracts. No roadmap; current behavior only.

## Pages and routes

| Route       | Page     | Description |
|------------|----------|--------------|
| `/`        | Home     | Description, disclaimer, nav to Trading and Portfolio |
| `/trading` | Trading  | CRUD trading entries; optional entry price; "Analyze now" AI block |
| `/portfolio` | Portfolio | CRUD positions; backend-computed fields (current price, upside %, etc.) |
| `/help`    | Help     | Static disclaimer and usage |

## Backend endpoints

Base URL: configurable (e.g. `http://localhost:8080`). All JSON where applicable.

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/securities?q=` | List securities (whitelist); optional query filter |
| GET | `/api/trading` | List trading entries |
| POST | `/api/trading` | Create trading entry (ticker, note?, entryPrice?) |
| PUT | `/api/trading/{id}` | Update trading entry |
| DELETE | `/api/trading/{id}` | Delete trading entry |
| GET | `/api/portfolio` | List portfolio positions (with computed fields) |
| POST | `/api/portfolio` | Create position |
| PUT | `/api/portfolio/{id}` | Update position |
| DELETE | `/api/portfolio/{id}` | Delete position |
| GET | `/api/prices?tickers=A,B` | USD prices; real provider when configured, else mock |
| POST | `/api/ai/pattern?tf=1h\|1d` | AI pattern analysis (see below) |

Swagger UI: `http://localhost:8080/swagger-ui/index.html`  
OpenAPI: `http://localhost:8080/v3/api-docs`

## AI: POST /api/ai/pattern

**Query:** `tf=1h` or `tf=1d`.

**Requirements:**
- `OPENAI_API_KEY` and `ALPHAVANTAGE_API_KEY` must be set in the backend environment.

**Error codes:**
- **400** — `OPENAI_API_KEY` is not configured. Body: JSON with `message` (e.g. "OPENAI_API_KEY is not configured").
- **502** — Market data provider not configured (`ALPHAVANTAGE_API_KEY` missing) or unavailable (rate limit, network, insufficient data). Body: JSON with `message` describing the issue.
- **502** — OpenAI call failed or returned invalid/unparseable response. Body: JSON with `message` (e.g. "AI returned invalid response" or similar).

**Success (200):** JSON body:
```json
{
  "timeframe": "1h" | "1d",
  "ticker": "string",
  "pattern": "string",
  "rationale": "string",
  "generatedAt": "ISO-8601 datetime"
}
```

**Caching:** Result is cached per timeframe for `ai.cacheTtlMinutes` (backend config) to reduce external calls.

**External dependencies:**
- OpenAI (Chat Completions) for pattern selection and rationale.
- Alpha Vantage for OHLC (1h: TIME_SERIES_INTRADAY 60min; 1d: TIME_SERIES_DAILY_ADJUSTED). Up to 5 tickers per analyze; candidate tickers rotate daily (UTC day offset into whitelist, wrap-around). Delay between requests to respect free-tier limits.

## Data and whitelist

- **Securities:** Table `securities` (ticker, company, sector), seeded from `sp500.csv`. Idempotent seed on startup.
- **Trading:** No currency field; entry price optional. Ticker must exist in `securities`.
- **Portfolio:** Positions with sector, company, ticker, buy/target price, quantity, currency (USD/EUR), notes. Computed fields in USD.
- **Whitelist:** AI and ticker validation use only tickers from `securities`.

## Market data and currency

- **Prices:** Backend can use Alpha Vantage for quotes when `ALPHAVANTAGE_API_KEY` is set; otherwise returns mock/deterministic USD prices.
- **Currency:** Portfolio allows USD/EUR input; calculations and storage normalized to USD. Fixed FX rate from config; no external FX API.
