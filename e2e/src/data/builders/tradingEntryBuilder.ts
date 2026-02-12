import { randomUUID } from 'node:crypto'

/**
 * Crypto-safe unique note. Prefix format "E2E|trading|w<workerIndex>|" for parallel safety.
 */
export function uniqueTradingNote(prefix?: string): string {
  const uuid = randomUUID()
  return prefix != null ? `${prefix}${uuid}` : `E2E|trading|${uuid}`
}

export type TradingEntryFormData = {
  ticker: string
  note: string
  entryPrice: string
}

export function tradingEntryBuilder(
  overrides: Partial<TradingEntryFormData> = {},
  prefix?: string
): TradingEntryFormData {
  return {
    ticker: 'AAPL',
    note: uniqueTradingNote(prefix),
    entryPrice: '100',
    ...overrides,
  }
}
