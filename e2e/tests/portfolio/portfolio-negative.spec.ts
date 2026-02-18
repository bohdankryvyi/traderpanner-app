import { test, expect } from '../../src/fixtures/baseFixture'
import { portfolioNegativeCases } from '../../src/data/datasets/portfolioCases'
import { getCountWithPrefix } from '../../src/utils/prefixCount'
import type { PortfolioPositionResponse } from '../../src/api/types'

const caseNames = portfolioNegativeCases('_').map((c) => c.name)

for (const name of caseNames) {
  test(`Portfolio negative: ${name}`, async ({ portfolioPage, portfolioApi }, testInfo) => {
    const prefix = `E2E|portfolio|w${testInfo.workerIndex}|`
    const { data } = portfolioNegativeCases(prefix).find((c) => c.name === name)!
    await portfolioApi.deleteE2EPortfolioAndVerify(prefix)
    await portfolioPage.goto()
    await portfolioPage.expectValidationErrorNotVisible()

    const countBefore = await getCountWithPrefix<PortfolioPositionResponse>(
      () => portfolioApi.getListResponse(),
      prefix,
      (p) => p.notes ?? ''
    )
    await portfolioPage.createPosition(data)
    await portfolioPage.expectValidationErrorVisible()
    await portfolioApi.assertNoPositionWithNotes(data.notes)
    const countAfter = await getCountWithPrefix<PortfolioPositionResponse>(
      () => portfolioApi.getListResponse(),
      prefix,
      (p) => p.notes ?? ''
    )
    expect(countAfter).toBe(countBefore)
  })
}
