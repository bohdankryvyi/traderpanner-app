/**
 * E2E URLs from env. Use E2E_BASE_URL (frontend) and E2E_API_URL (backend).
 */
const DEFAULT_BASE_URL = 'http://localhost:5173'
const DEFAULT_API_URL = 'http://localhost:8080'

export function getBaseUrl(): string {
  return process.env.E2E_BASE_URL ?? process.env.PLAYWRIGHT_BASE_URL ?? DEFAULT_BASE_URL
}

export function getApiBaseUrl(): string {
  return process.env.E2E_API_URL ?? process.env.API_BASE_URL ?? DEFAULT_API_URL
}
