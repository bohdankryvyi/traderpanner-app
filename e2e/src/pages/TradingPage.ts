import { expect } from '@playwright/test'
import type { Page } from '@playwright/test'
import type { TradingEntryFormData } from '../data/builders/tradingEntryBuilder'

const FORM_ERROR_TESTID = 'trading-form-error'

export class TradingPage {
  constructor(private readonly page: Page) {}

  async goto(): Promise<void> {
    await this.page.goto('/trading')
  }

  async reload(): Promise<void> {
    await this.page.reload()
  }

  async createEntry(data: TradingEntryFormData): Promise<void> {
    await this.page.getByTestId('trading-ticker').fill(data.ticker)
    await this.page.getByTestId('trading-note').fill(data.note)
    await this.page.getByTestId('trading-entry-price').fill(data.entryPrice)
    await this.page.getByTestId('trading-form-submit').click()
  }

  async expectRowVisibleById(id: number): Promise<void> {
    await this.page.getByTestId('trading-row-' + id).waitFor({ state: 'visible' })
  }

  async expectRowNotVisibleById(id: number): Promise<void> {
    await this.page.getByTestId('trading-row-' + id).waitFor({ state: 'detached' })
  }

  async editById(id: number, data: Partial<TradingEntryFormData>): Promise<void> {
    await this.page.getByTestId('trading-row-' + id).getByTestId('trading-row-edit').click()
    if (data.ticker != null) await this.page.getByTestId('trading-ticker').fill(data.ticker)
    if (data.note != null) await this.page.getByTestId('trading-note').fill(data.note)
    if (data.entryPrice != null) await this.page.getByTestId('trading-entry-price').fill(data.entryPrice)
    await this.page.getByTestId('trading-form-submit').click()
  }

  async deleteById(id: number): Promise<void> {
    await this.page.getByTestId('trading-row-' + id).getByTestId('trading-row-delete').click()
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
      throw new Error('expectValidationErrorVisible failed: no element with data-testid=' + FORM_ERROR_TESTID + ' found in DOM')
    }
    await expect(locator.first()).toBeVisible()
  }
}
