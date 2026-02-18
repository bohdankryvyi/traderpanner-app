import type { TradingEntryFormData } from '../builders/tradingEntryBuilder'
import { tradingEntryBuilder } from '../builders/tradingEntryBuilder'

/**
 * Valid default for CRUD. Entry price is optional; only ticker is required.
 */
export const tradingValidDefault = (prefix?: string): TradingEntryFormData =>
  tradingEntryBuilder({}, prefix)

export type TradingNegativeCase = {
  name: string
  build: (prefix: string) => TradingEntryFormData
}

/**
 * Negative cases: mandatory fields only (ticker required). Use case.build(prefix) once per test.
 */
export function tradingNegativeCases(): TradingNegativeCase[] {
  return [
    {
      name: 'missing required ticker',
      build: (prefix) => tradingEntryBuilder({ ticker: '' }, prefix),
    },
  ]
}
