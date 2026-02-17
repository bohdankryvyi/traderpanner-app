export type Currency = 'USD' | 'EUR'

export type SecurityDto = {
  ticker: string
  company: string
  sector: string
}

export type TradingEntryRequest = {
  ticker: string
  note?: string | null
  entryPrice?: number | null
}

export type TradingEntryResponse = {
  id: number
  ticker: string
  note?: string | null
  entryPrice: number | null
  currentPriceUsd: number
}

export type PortfolioPositionRequest = {
  sector: string
  company: string
  ticker: string
  buyPrice: number
  targetPrice?: number | null
  quantity: number
  currency: Currency
  notes?: string | null
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

export type PricesResponse = Record<string, number>

export type AiTipsResponse = {
  timeframe: '1h' | '1d'
  tipsText: string
  generatedAt: string
}

export type PatternResponse = {
  timeframe: '1h' | '1d'
  ticker: string
  pattern: string
  rationale: string
  generatedAt: string
  /** e.g. "openai", "openai-no-market-data", "fallback", "fallback-rate-limit", "fallback-openai-error", "fallback-openai-invalid" */
  source?: string
}

export type ApiErrorResponse = {
  timestamp: string
  status: number
  error: string
  message: string
  path: string
}

