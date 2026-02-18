import type { TestInfo } from '@playwright/test'
import { E2E_PREFIX_BASE, E2E_SEP } from '../constants/e2e'
import type { E2EDomain } from '../constants/e2e'

/**
 * Builds a unique E2E prefix for a test: base + domain + worker + per-test id.
 * Avoids collisions between tests in the same worker (e.g. parallel runs).
 * Format: E2E|domain|w<workerIndex>|p<parallelIndex>|
 */
export function buildE2EPrefix(domain: E2EDomain, testInfo: TestInfo): string {
  const worker = testInfo.workerIndex
  const perTest = testInfo.parallelIndex
  return `${E2E_PREFIX_BASE}${E2E_SEP}${domain}${E2E_SEP}w${worker}${E2E_SEP}p${perTest}${E2E_SEP}`
}
