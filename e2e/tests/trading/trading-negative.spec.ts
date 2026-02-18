import { test, expect } from '../../src/fixtures/baseFixture'
import { tradingNegativeCases } from '../../src/data/datasets/tradingCases'
import { getCountWithPrefix } from '../../src/utils/prefixCount'
import type { TradingEntryResponse } from '../../src/api/types'

const caseNames = tradingNegativeCases('_').map((c) => c.name)

for (const name of caseNames) {
  test(`Trading negative: ${name}`, async ({ tradingPage, tradingApi }, testInfo) => {
    const prefix = `E2E|trading|w${testInfo.workerIndex}|`
    const { data } = tradingNegativeCases(prefix).find((c) => c.name === name)!
    await tradingApi.deleteE2ETradingAndVerify(prefix)
    await tradingPage.goto()
    await tradingPage.expectValidationErrorNotVisible()

    const countBefore = await getCountWithPrefix<TradingEntryResponse>(
      () => tradingApi.getListResponse(),
      prefix,
      (e) => e.note ?? ''
    )
    await tradingPage.createEntry(data)
    await tradingPage.expectValidationErrorVisible()
    await tradingApi.assertNoEntryWithNote(data.note)
    const countAfter = await getCountWithPrefix<TradingEntryResponse>(
      () => tradingApi.getListResponse(),
      prefix,
      (e) => e.note ?? ''
    )
    expect(countAfter).toBe(countBefore)
  })
}
