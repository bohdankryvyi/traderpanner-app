import { test } from '../../src/fixtures/baseFixture'
import { tradingEntryBuilder } from '../../src/data/builders/tradingEntryBuilder'
import { createTradingEntryAndResolveId } from '../../src/utils/crudHelpers'
import { runCrudFlow } from '../../src/utils/crudFlow'
import { buildE2EPrefix } from '../../src/utils/e2ePrefix'
import { DOMAIN_TRADING } from '../../src/constants/e2e'

test.describe('Trading @trading', () => {
  test('CRUD: create -> edit -> delete, then no E2E entries with prefix remain @crud', async ({
    tradingPage,
    tradingApi,
  }, testInfo) => {
    const prefix = buildE2EPrefix(DOMAIN_TRADING, testInfo)
    const createData = tradingEntryBuilder({}, prefix)
    const editPatch = { note: tradingEntryBuilder({}, prefix).note }

    await runCrudFlow({
      page: tradingPage,
      api: tradingApi,
      createData,
      editPatch,
      createAndResolveId: createTradingEntryAndResolveId,
      cleanup: () => tradingApi.deleteE2ETradingAndVerify(prefix),
      assertNoPrefix: () => tradingApi.assertNoEntriesWithPrefix(prefix),
    })
  })
})
