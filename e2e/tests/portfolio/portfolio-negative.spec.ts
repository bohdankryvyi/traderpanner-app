import { test, expect } from '../../src/fixtures/baseFixture'
import { portfolioNegativeCases } from '../../src/data/datasets/portfolioCases'

const caseNames = portfolioNegativeCases('_').map((c) => c.name)

for (const name of caseNames) {
  test(`Portfolio negative: ${name}`, async ({ portfolioPage, portfolioApi }, testInfo) => {
    const prefix = `E2E|portfolio|w${testInfo.workerIndex}|`
    const { data } = portfolioNegativeCases(prefix).find((c) => c.name === name)!
    await portfolioApi.deleteE2EPortfolioAndVerify(prefix)
    await portfolioPage.goto()
    await portfolioPage.expectValidationErrorNotVisible()

    const listBefore = (await portfolioApi.getListResponse()).body
    const beforePrefixCount = listBefore.filter((p) => (p.notes ?? '').startsWith(prefix)).length
    await portfolioPage.createPosition(data)
    await portfolioPage.expectValidationErrorVisible()
    await portfolioApi.assertNoPositionWithNotes(data.notes)
    const listAfter = (await portfolioApi.getListResponse()).body
    const afterPrefixCount = listAfter.filter((p) => (p.notes ?? '').startsWith(prefix)).length
    expect(afterPrefixCount).toBe(beforePrefixCount)
  })
}
