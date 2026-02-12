import { test, expect } from '../../src/fixtures/baseFixture'
import { tradingNegativeCases } from '../../src/data/datasets/tradingCases'

const caseNames = tradingNegativeCases('_').map((c) => c.name)

for (const name of caseNames) {
  test(`Trading negative: ${name}`, async ({ tradingPage, tradingApi }, testInfo) => {
    const prefix = `E2E|trading|w${testInfo.workerIndex}|`
    const { data } = tradingNegativeCases(prefix).find((c) => c.name === name)!
    await tradingApi.deleteE2ETradingAndVerify(prefix)
    await tradingPage.goto()
    await tradingPage.expectValidationErrorNotVisible()

    const listBefore = (await tradingApi.getListResponse()).body
    const beforePrefixCount = listBefore.filter((e) => (e.note ?? '').startsWith(prefix)).length
    await tradingPage.createEntry(data)
    await tradingPage.expectValidationErrorVisible()
    await tradingApi.assertNoEntryWithNote(data.note)
    const listAfter = (await tradingApi.getListResponse()).body
    const afterPrefixCount = listAfter.filter((e) => (e.note ?? '').startsWith(prefix)).length
    expect(afterPrefixCount).toBe(beforePrefixCount)
  })
}
