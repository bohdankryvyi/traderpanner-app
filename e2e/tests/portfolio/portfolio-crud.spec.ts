import { test } from '../../src/fixtures/baseFixture'
import { portfolioPositionBuilder } from '../../src/data/builders/portfolioPositionBuilder'
import { createPortfolioPositionAndResolveId } from '../../src/utils/crudHelpers'

test('Portfolio CRUD: create -> edit -> delete, then no E2E positions with prefix remain', async (
  { portfolioPage, portfolioApi },
  testInfo
) => {
  const prefix = `E2E|portfolio|w${testInfo.workerIndex}|`

  await test.step('Arrange: prefix-only cleanup', async () => {
    await portfolioApi.deleteE2EPortfolioAndVerify(prefix)
  })

  const data = portfolioPositionBuilder({}, prefix)
  const updatedNotes = portfolioPositionBuilder({}, prefix).notes

  const rowId = await test.step('Create and resolve id', async () => {
    return createPortfolioPositionAndResolveId(portfolioPage, portfolioApi, data)
  })

  await test.step('Edit', async () => {
    await portfolioPage.expectRowVisibleById(rowId)
    await portfolioPage.editById(rowId, { notes: updatedNotes, quantity: '2' })
  })

  await test.step('Delete', async () => {
    await portfolioPage.expectRowVisibleById(rowId)
    await portfolioPage.deleteById(rowId)
    await portfolioPage.expectRowNotVisibleById(rowId)
  })

  await test.step('API verify: no positions with prefix', async () => {
    await portfolioApi.assertNoPositionsWithPrefix(prefix)
  })
})
