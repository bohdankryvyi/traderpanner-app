import type { PortfolioPositionFormData } from '../builders/portfolioPositionBuilder'
import { portfolioPositionBuilder } from '../builders/portfolioPositionBuilder'

/**
 * Valid default for CRUD.
 */
export const portfolioValidDefault = (prefix?: string): PortfolioPositionFormData =>
  portfolioPositionBuilder({}, prefix)

export type PortfolioNegativeCase = {
  name: string
  build: (prefix: string) => PortfolioPositionFormData
}

/**
 * Negative cases: mandatory fields only (sector, company, ticker, buyPrice, quantity per DTO).
 * Use case.build(prefix) once per test to get unique notes; no need to call builder twice or find by name.
 */
export function portfolioNegativeCases(): PortfolioNegativeCase[] {
  return [
    { name: 'missing sector', build: (prefix) => portfolioPositionBuilder({ sector: '' }, prefix) },
    {
      name: 'missing company',
      build: (prefix) => portfolioPositionBuilder({ company: '' }, prefix),
    },
    { name: 'missing ticker', build: (prefix) => portfolioPositionBuilder({ ticker: '' }, prefix) },
    {
      name: 'missing buyPrice',
      build: (prefix) => portfolioPositionBuilder({ buyPrice: '' }, prefix),
    },
    {
      name: 'missing quantity',
      build: (prefix) => portfolioPositionBuilder({ quantity: '' }, prefix),
    },
  ]
}
