import type { TradingEntryFormData } from '../builders/tradingEntryBuilder'
import { tradingEntryBuilder } from '../builders/tradingEntryBuilder'

/**
 * Valid default for CRUD. Entry price is optional; only ticker is required.
 */
export const tradingValidDefault = (prefix?: string): TradingEntryFormData => tradingEntryBuilder({}, prefix)

/**
 * Negative cases: mandatory fields only (ticker required). Each case has a unique note with prefix for API assertion.
 */
export function tradingNegativeCases(prefix: string): Array<{ name: string; data: TradingEntryFormData }> {
  return [
    {
      name: 'missing required ticker',
      data: tradingEntryBuilder({ ticker: '' }, prefix),
    },
  ]
}
