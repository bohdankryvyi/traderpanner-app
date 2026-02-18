import { getBaseUrl, getApiBaseUrl } from './src/config/env'

const TIMEOUT_MS = 15_000

async function fetchOk(url: string): Promise<{ ok: boolean; status?: number; error?: string }> {
  try {
    const res = await fetch(url, {
      method: 'GET',
      signal: AbortSignal.timeout(TIMEOUT_MS),
      headers: { Accept: 'application/json, text/html' },
    })
    return { ok: res.ok, status: res.status }
  } catch (e) {
    const err = e instanceof Error ? e.message : String(e)
    return { ok: false, error: err }
  }
}

export default async function globalSetup(): Promise<void> {
  const baseUrl = getBaseUrl()
  const apiUrl = getApiBaseUrl()

  const [front, back] = await Promise.all([
    fetchOk(baseUrl.replace(/\/$/, '') + '/'),
    fetchOk(apiUrl.replace(/\/$/, '') + '/api/portfolio'),
  ])

  const failures: string[] = []
  if (!front.ok) {
    failures.push(
      `Frontend at ${baseUrl} is not ready (status=${front.status ?? 'N/A'}${front.error ? `, error=${front.error}` : ''}).`
    )
  }
  if (!back.ok) {
    failures.push(
      `Backend at ${apiUrl} is not ready (status=${back.status ?? 'N/A'}${back.error ? `, error=${back.error}` : ''}).`
    )
  }

  if (failures.length > 0) {
    const hint = [
      'Start services before running E2E:',
      '  Backend:  cd app-backend && ./mvnw spring-boot:run  (or mvnw.cmd on Windows)',
      '  Frontend: cd app-frontend && npm run dev',
      'Override URLs with env: E2E_BASE_URL, E2E_API_URL (or PLAYWRIGHT_BASE_URL, API_BASE_URL).',
    ].join('\n')
    throw new Error(`E2E preflight failed.\n\n${failures.join('\n')}\n\n${hint}`)
  }
}
