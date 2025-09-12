# TraderPlanner — App

Monorepo for the app:
- `app-backend` — Spring Boot API (Java 21, SB 3.x)
- `app-frontend` — React + TypeScript + Vite
- `wiremock` — offline stubs for market/FX APIs (dev & tests)

## Local quick start (will evolve)
Prerequisites: JDK 21, Node 20, Docker
- Clone repo
- Create `.env` from `.env.example` (we'll add later)
- Start DB & stubs via Docker Compose (we'll add later)
- Run backend (mock profile), then frontend dev server

## Planned pages
- Home: header statuses, 2 big buttons, Help
- Trading: form (ticker/label/entry/currency), table with Current price (USD), AI "Analyze now"
- Portfolio: form (ticker/entry/currency/qty/notes), table with target/upside/P&L/value

## Tech stack (MVP)
BE: Java 21, Spring Boot 3.x, Flyway, springdoc, Bucket4j  
DB: PostgreSQL 16  
FE: React 18 + TS 5 + Vite (i18n EN/UA, responsive)  
AI: on-demand tips (cached 15–30m)  
Tests: JUnit/Testcontainers, Playwright (separate repo), k6  
Infra: Docker Compose, GitHub Actions (later), Azure (later)
