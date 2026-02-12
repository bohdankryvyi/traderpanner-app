import { randomUUID } from 'node:crypto'
import type { Currency } from '../../api/types'

/**
 * Crypto-safe unique notes. Prefix format "E2E|portfolio|w<workerIndex>|" for parallel safety.
 */
export function uniquePortfolioNotes(prefix?: string): string {
  const uuid = randomUUID()
  return prefix != null ? `${prefix}${uuid}` : `E2E|portfolio|${uuid}`
}

export type PortfolioPositionFormData = {
  sector: string
  company: string
  ticker: string
  buyPrice: string
  targetPrice: string
  quantity: string
  currency: Currency
  notes: string
}

export function portfolioPositionBuilder(
  overrides: Partial<PortfolioPositionFormData> = {},
  prefix?: string
): PortfolioPositionFormData {
  return {
    sector: 'Technology',
    company: 'Apple Inc',
    ticker: 'AAPL',
    buyPrice: '100',
    targetPrice: '120',
    quantity: '1',
    currency: 'USD',
    notes: uniquePortfolioNotes(prefix),
    ...overrides,
  }
}
