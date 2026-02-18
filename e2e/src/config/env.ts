/**
 * E2E URL config from env. No hardcoded localhost in tests or API client.
 */
const DEFAULT_BASE_URL = 'http://localhost:5173'
const DEFAULT_API_BASE_URL = 'http://localhost:8080'

export function getBaseURL(): string {
  return process.env.E2E_BASE_URL ?? process.env.PLAYWRIGHT_BASE_URL ?? DEFAULT_BASE_URL
}

export function getApiBaseURL(): string {
  return process.env.E2E_API_BASE_URL ?? process.env.API_BASE_URL ?? DEFAULT_API_BASE_URL
}
