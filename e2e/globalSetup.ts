/**
 * Preflight: frontend and backend must be reachable before any test.
 * Retry loops, backend fallbacks, clear errors. Fail within ~60s total.
 */
import { getBaseURL, getApiBaseURL } from './src/config/env'
import { waitForHttpOk } from './src/utils/preflight'

const FRONTEND_TIMEOUT_MS = 45_000
const FRONTEND_INTERVAL_MS = 1_500

const BACKEND_ENDPOINTS: Array<{ path: string; label: string; bodyOk?: (text: string) => boolean }> = [
  { path: '/actuator/health', label: 'actuator/health', bodyOk: (t) => t.includes('"status":"UP"') },
  { path: '/swagger-ui/index.html', label: 'swagger-ui/index.html' },
  { path: '/api/portfolio', label: 'api/portfolio' },
  { path: '/api/trading', label: 'api/trading' },
]
const BACKEND_TIMEOUTS_MS = [15_000, 10_000, 10_000, 10_000]
const BACKEND_INTERVAL_MS = 1_500

async function checkFrontend(): Promise<void> {
  const baseURL = getBaseURL()
  try {
    await waitForHttpOk({
      url: baseURL,
      label: 'Frontend',
      timeoutMs: FRONTEND_TIMEOUT_MS,
      intervalMs: FRONTEND_INTERVAL_MS,
    })
  } catch (e) {
    const msg = e instanceof Error ? e.message : String(e)
    throw new Error(
      `${msg}\n\n` +
        `Frontend must be running. Set E2E_BASE_URL or PLAYWRIGHT_BASE_URL if not using default (http://localhost:5173).\n` +
        `Check the terminal where the frontend dev server is running.`
    )
  }
}

async function checkBackend(): Promise<void> {
  const apiBase = getApiBaseURL()
  const errors: Array<{ endpoint: string; error: string }> = []

  for (let i = 0; i < BACKEND_ENDPOINTS.length; i++) {
    const { path, label, bodyOk } = BACKEND_ENDPOINTS[i]
    const url = `${apiBase}${path}`
    const timeoutMs = BACKEND_TIMEOUTS_MS[i]
    try {
      await waitForHttpOk({
        url,
        label: `Backend (${label})`,
        timeoutMs,
        intervalMs: BACKEND_INTERVAL_MS,
        bodyOk,
      })
      return
    } catch (e) {
      const error = e instanceof Error ? e.message : String(e)
      errors.push({ endpoint: `${path} (${label})`, error })
    }
  }

  const details = errors.map(({ endpoint, error }) => `  - ${endpoint}: ${error}`).join('\n')
  throw new Error(
    `Preflight failed: Backend unreachable.\n` +
      `  Base URL: ${apiBase}\n` +
      `  Endpoints tried (in order):\n${details}\n\n` +
      `Start the backend (Spring Boot) and ensure it listens on E2E_API_BASE_URL (default 8080).\n` +
      `If running in CI, ensure the workflow starts the backend and Postgres and ports match.\n` +
      `Check the terminal/logs where the backend is running.`
  )
}

async function globalSetup(): Promise<void> {
  await checkFrontend()
  await checkBackend()
}

export default globalSetup
