import { test, expect } from '../../src/fixtures/baseFixture'
import { portfolioNegativeCases } from '../../src/data/datasets/portfolioCases'
import { buildE2EPrefix } from '../../src/utils/e2ePrefix'
import { countByPrefix } from '../../src/utils/prefixCount'
import { DOMAIN_PORTFOLIO } from '../../src/constants/e2e'
import type { PortfolioPositionResponse } from '../../src/api/types'

const cases = portfolioNegativeCases()

test.describe('Portfolio @portfolio', () => {
  for (const negCase of cases) {
    test(`negative: ${negCase.name} @negative`, async ({
      portfolioPage,
      portfolioApi,
    }, testInfo) => {
      const prefix = buildE2EPrefix(DOMAIN_PORTFOLIO, testInfo)
      const data = negCase.build(prefix)

      await test.step('Arrange: cleanup and open form', async () => {
        await portfolioApi.deleteE2EPortfolioAndVerify(prefix)
        await portfolioPage.goto()
        await portfolioPage.expectValidationErrorNotVisible()
      })

      const countBefore = await test.step('Act: get count before submit', () =>
        countByPrefix<PortfolioPositionResponse>(
          () => portfolioApi.getListResponse(),
          prefix,
          (p) => p.notes ?? ''
        ))

      await test.step('Act: submit invalid data', () => portfolioPage.createPosition(data))

      await test.step('Assert: validation visible and no position created', async () => {
        await portfolioPage.expectValidationErrorVisible()
        await portfolioApi.assertNoPositionWithNotes(data.notes)
        const countAfter = await countByPrefix<PortfolioPositionResponse>(
          () => portfolioApi.getListResponse(),
          prefix,
          (p) => p.notes ?? ''
        )
        expect(countAfter).toBe(countBefore)
      })
    })
  }
})
