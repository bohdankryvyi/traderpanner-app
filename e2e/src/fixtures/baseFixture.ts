import { test as base } from '@playwright/test'
import { createApiClient } from '../api/apiClient'
import { createTradingApi } from '../api/tradingApi'
import { createPortfolioApi } from '../api/portfolioApi'
import { getApiBaseUrl } from '../config/env'
import { TradingPage } from '../pages/TradingPage'
import { PortfolioPage } from '../pages/PortfolioPage'
import type { TradingApi } from '../api/tradingApi'
import type { PortfolioApi } from '../api/portfolioApi'

type CustomFixtures = {
  tradingPage: TradingPage
  portfolioPage: PortfolioPage
  tradingApi: TradingApi
  portfolioApi: PortfolioApi
}

export const test = base.extend<CustomFixtures>({
  tradingPage: async ({ page }, use) => {
    await use(new TradingPage(page))
  },
  portfolioPage: async ({ page }, use) => {
    await use(new PortfolioPage(page))
  },
  tradingApi: async ({ request }, use) => {
    const api = createApiClient(request, getApiBaseUrl())
    await use(createTradingApi(api))
  },
  portfolioApi: async ({ request }, use) => {
    const api = createApiClient(request, getApiBaseUrl())
    await use(createPortfolioApi(api))
  },
})

export { expect } from '@playwright/test'
