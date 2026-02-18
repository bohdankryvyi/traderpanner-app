import { test } from '../../src/fixtures/baseFixture'
import { portfolioPositionBuilder } from '../../src/data/builders/portfolioPositionBuilder'
import { createPortfolioPositionAndResolveId } from '../../src/utils/crudHelpers'
import { runCrudFlow } from '../../src/utils/crudFlow'
import { buildE2EPrefix } from '../../src/utils/e2ePrefix'
import { DOMAIN_PORTFOLIO } from '../../src/constants/e2e'

test.describe('Portfolio @portfolio', () => {
  test('CRUD: create -> edit -> delete, then no E2E positions with prefix remain @crud', async ({
    portfolioPage,
    portfolioApi,
  }, testInfo) => {
    const prefix = buildE2EPrefix(DOMAIN_PORTFOLIO, testInfo)
    const createData = portfolioPositionBuilder({}, prefix)
    const editPatch = {
      notes: portfolioPositionBuilder({}, prefix).notes,
      quantity: '2',
    }

    await runCrudFlow({
      page: portfolioPage,
      api: portfolioApi,
      createData,
      editPatch,
      createAndResolveId: createPortfolioPositionAndResolveId,
      cleanup: () => portfolioApi.deleteE2EPortfolioAndVerify(prefix),
      assertNoPrefix: () => portfolioApi.assertNoPositionsWithPrefix(prefix),
    })
  })
})
