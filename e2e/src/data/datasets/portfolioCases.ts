import type { PortfolioPositionFormData } from '../builders/portfolioPositionBuilder'
import { portfolioPositionBuilder } from '../builders/portfolioPositionBuilder'

/**
 * Valid default for CRUD.
 */
export const portfolioValidDefault = (prefix?: string): PortfolioPositionFormData =>
  portfolioPositionBuilder({}, prefix)

/**
 * Negative cases: mandatory fields only (sector, company, ticker, buyPrice, quantity per DTO).
 * Each case has unique notes with prefix for API assertion.
 */
export function portfolioNegativeCases(prefix: string): Array<{ name: string; data: PortfolioPositionFormData }> {
  return [
    { name: 'missing sector', data: portfolioPositionBuilder({ sector: '' }, prefix) },
    { name: 'missing company', data: portfolioPositionBuilder({ company: '' }, prefix) },
    { name: 'missing ticker', data: portfolioPositionBuilder({ ticker: '' }, prefix) },
    { name: 'missing buyPrice', data: portfolioPositionBuilder({ buyPrice: '' }, prefix) },
    { name: 'missing quantity', data: portfolioPositionBuilder({ quantity: '' }, prefix) },
  ]
}
