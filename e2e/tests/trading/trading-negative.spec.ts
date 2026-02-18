import { test, expect } from '../../src/fixtures/baseFixture'
import { tradingNegativeCases } from '../../src/data/datasets/tradingCases'
import { buildE2EPrefix } from '../../src/utils/e2ePrefix'
import { countByPrefix } from '../../src/utils/prefixCount'
import { DOMAIN_TRADING } from '../../src/constants/e2e'
import type { TradingEntryResponse } from '../../src/api/types'

const cases = tradingNegativeCases()

test.describe('Trading @trading', () => {
  for (const negCase of cases) {
    test(`negative: ${negCase.name} @negative`, async ({ tradingPage, tradingApi }, testInfo) => {
      const prefix = buildE2EPrefix(DOMAIN_TRADING, testInfo)
      const data = negCase.build(prefix)

      await test.step('Arrange: cleanup and open form', async () => {
        await tradingApi.deleteE2ETradingAndVerify(prefix)
        await tradingPage.goto()
        await tradingPage.expectValidationErrorNotVisible()
      })

      const countBefore = await test.step('Act: get count before submit', () =>
        countByPrefix<TradingEntryResponse>(
          () => tradingApi.getListResponse(),
          prefix,
          (e) => e.note ?? ''
        ))

      await test.step('Act: submit invalid data', () => tradingPage.createEntry(data))

      await test.step('Assert: validation visible and no entry created', async () => {
        await tradingPage.expectValidationErrorVisible()
        await tradingApi.assertNoEntryWithNote(data.note)
        const countAfter = await countByPrefix<TradingEntryResponse>(
          () => tradingApi.getListResponse(),
          prefix,
          (e) => e.note ?? ''
        )
        expect(countAfter).toBe(countBefore)
      })
    })
  }
})
