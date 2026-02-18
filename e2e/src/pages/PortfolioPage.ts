import { expect } from '@playwright/test'
import type { Page } from '@playwright/test'
import type { PortfolioPositionFormData } from '../data/builders/portfolioPositionBuilder'

const FORM_ERROR_TESTID = 'portfolio-form-error'

export class PortfolioPage {
  constructor(private readonly page: Page) {}

  async goto(): Promise<void> {
    await this.page.goto('/portfolio')
  }

  async reload(): Promise<void> {
    await this.page.reload()
  }

  async createPosition(data: PortfolioPositionFormData): Promise<void> {
    await this.page.getByTestId('portfolio-sector').fill(data.sector)
    await this.page.getByTestId('portfolio-company').fill(data.company)
    await this.page.getByTestId('portfolio-ticker').fill(data.ticker)
    await this.page.getByTestId('portfolio-buy-price').fill(data.buyPrice)
    await this.page.getByTestId('portfolio-target-price').fill(data.targetPrice)
    await this.page.getByTestId('portfolio-quantity').fill(data.quantity)
    await this.page.getByTestId('portfolio-notes').fill(data.notes)
    await this.page.getByTestId('portfolio-form-submit').click()
  }

  async expectRowVisibleById(id: number): Promise<void> {
    await this.page.getByTestId(`portfolio-row-${id}`).waitFor({ state: 'visible' })
  }

  async expectRowNotVisibleById(id: number): Promise<void> {
    await this.page.getByTestId(`portfolio-row-${id}`).waitFor({ state: 'detached' })
  }

  async editById(id: number, data: Partial<PortfolioPositionFormData>): Promise<void> {
    await this.page.getByTestId(`portfolio-row-${id}`).getByTestId('portfolio-row-edit').click()
    if (data.sector != null) await this.page.getByTestId('portfolio-sector').fill(data.sector)
    if (data.company != null) await this.page.getByTestId('portfolio-company').fill(data.company)
    if (data.ticker != null) await this.page.getByTestId('portfolio-ticker').fill(data.ticker)
    if (data.buyPrice != null)
      await this.page.getByTestId('portfolio-buy-price').fill(data.buyPrice)
    if (data.targetPrice != null)
      await this.page.getByTestId('portfolio-target-price').fill(data.targetPrice)
    if (data.quantity != null) await this.page.getByTestId('portfolio-quantity').fill(data.quantity)
    if (data.notes != null) await this.page.getByTestId('portfolio-notes').fill(data.notes)
    await this.page.getByTestId('portfolio-form-submit').click()
  }

  async deleteById(id: number): Promise<void> {
    await this.page.getByTestId(`portfolio-row-${id}`).getByTestId('portfolio-row-delete').click()
  }

  async expectValidationErrorNotVisible(): Promise<void> {
    const locator = this.page.getByTestId(FORM_ERROR_TESTID)
    const count = await locator.count()
    if (count === 0) return
    await expect(locator.first()).not.toBeVisible()
  }

  async expectValidationErrorVisible(): Promise<void> {
    const locator = this.page.getByTestId(FORM_ERROR_TESTID)
    const count = await locator.count()
    if (count === 0) {
      throw new Error(
        'expectValidationErrorVisible failed: no element with data-testid=' +
          FORM_ERROR_TESTID +
          ' found in DOM'
      )
    }
    await expect(locator.first()).toBeVisible()
  }
}
