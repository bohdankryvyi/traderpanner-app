/**
 * Minimal DTOs for E2E API layer (mirrors backend contracts).
 */
export type Currency = 'USD' | 'EUR'

export type TradingEntryResponse = {
  id: number
  ticker: string
  note?: string | null
  entryPrice: number | null
  currentPriceUsd: number
}

export type PortfolioPositionResponse = {
  id: number
  sector: string
  company: string
  ticker: string
  buyPrice: number
  targetPrice?: number | null
  quantity: number
  currency: Currency
  notes?: string | null
  currentPriceUsd: number
  upsidePercent: number
  investedAmountUsd: number
  sharePercent: number
  expectedProfitUsd: number
  profitSharePercent: number
}

export type ApiErrorResponse = {
  message: string
  status?: number
  error?: string
  path?: string
}
