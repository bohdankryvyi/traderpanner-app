/**
 * Retry-loop HTTP readiness check. Used by globalSetup preflight.
 */

export type WaitForHttpOkOpts = {
  url: string
  label: string
  timeoutMs: number
  intervalMs: number
  perRequestTimeoutMs?: number
  /** If set, success requires 2xx AND bodyOk(bodyText). Used e.g. for actuator/health. */
  bodyOk?: (bodyText: string) => boolean
}

const DEFAULT_PER_REQUEST_MS = 10_000

/**
 * GET url in a loop until 2xx (and optional body check) or timeoutMs elapsed.
 * On timeout throws Error with label, url, timeout, interval, and lastError.
 */
export async function waitForHttpOk(opts: WaitForHttpOkOpts): Promise<void> {
  const {
    url,
    label,
    timeoutMs,
    intervalMs,
    perRequestTimeoutMs = DEFAULT_PER_REQUEST_MS,
    bodyOk,
  } = opts
  const start = Date.now()
  let lastError: string | null = null

  while (Date.now() - start < timeoutMs) {
    const controller = new AbortController()
    const t = setTimeout(() => controller.abort(), perRequestTimeoutMs)
    try {
      const res = await fetch(url, { signal: controller.signal })
      clearTimeout(t)
      if (!res.ok) {
        lastError = `${res.status} ${res.statusText}`
      } else if (bodyOk) {
        const text = await res.text()
        if (bodyOk(text)) return
        lastError = '200 but body check failed'
      } else {
        return
      }
    } catch (e) {
      clearTimeout(t)
      lastError = e instanceof Error ? e.message : String(e)
    }
    if (Date.now() - start >= timeoutMs) break
    await new Promise((r) => setTimeout(r, intervalMs))
  }

  throw new Error(
    `Preflight failed: ${label}\n` +
      `  URL: ${url}\n` +
      `  Timeout: ${timeoutMs}ms, interval: ${intervalMs}ms\n` +
      `  Last error: ${lastError ?? 'unknown'}`
  )
}
