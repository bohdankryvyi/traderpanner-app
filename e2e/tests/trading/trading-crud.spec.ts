import { test } from '../../src/fixtures/baseFixture'
import { tradingEntryBuilder } from '../../src/data/builders/tradingEntryBuilder'
import { createTradingEntryAndResolveId } from '../../src/utils/crudHelpers'

test('Trading CRUD: create -> edit -> delete, then no E2E entries with prefix remain', async (
  { tradingPage, tradingApi },
  testInfo
) => {
  const prefix = `E2E|trading|w${testInfo.workerIndex}|`

  await test.step('Arrange: prefix-only cleanup', async () => {
    await tradingApi.deleteE2ETradingAndVerify(prefix)
  })

  const data = tradingEntryBuilder({}, prefix)
  const updatedNote = tradingEntryBuilder({}, prefix).note

  const rowId = await test.step('Create and resolve id', async () => {
    return createTradingEntryAndResolveId(tradingPage, tradingApi, data)
  })

  await test.step('Edit', async () => {
    await tradingPage.expectRowVisibleById(rowId)
    await tradingPage.editById(rowId, { note: updatedNote })
  })

  await test.step('Delete', async () => {
    await tradingPage.expectRowVisibleById(rowId)
    await tradingPage.deleteById(rowId)
    await tradingPage.expectRowNotVisibleById(rowId)
  })

  await test.step('API verify: no entries with prefix', async () => {
    await tradingApi.assertNoEntriesWithPrefix(prefix)
  })
})
