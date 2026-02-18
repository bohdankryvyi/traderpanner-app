import { test } from '@playwright/test'

/**
 * Generic CRUD flow: cleanup -> create -> edit -> delete -> API verify.
 * Keeps portfolio and trading CRUD tests DRY and consistent.
 */
export interface CrudFlowPage<TFormData> {
  expectRowVisibleById(id: number): Promise<void>
  expectRowNotVisibleById(id: number): Promise<void>
  editById(id: number, patch: Partial<TFormData>): Promise<void>
  deleteById(id: number): Promise<void>
}

export interface CrudFlowOptions<TFormData, TApi = unknown> {
  page: CrudFlowPage<TFormData>
  api: TApi
  createData: TFormData
  editPatch: Partial<TFormData>
  createAndResolveId: (page: CrudFlowPage<TFormData>, api: TApi, data: TFormData) => Promise<number>
  cleanup: () => Promise<void>
  assertNoPrefix: () => Promise<void>
}

export async function runCrudFlow<TFormData, TApi = unknown>(
  opts: CrudFlowOptions<TFormData, TApi>
): Promise<void> {
  const { page, api, createData, editPatch, createAndResolveId, cleanup, assertNoPrefix } = opts

  await test.step('Arrange: prefix-only cleanup', cleanup)

  const rowId = await test.step('Create and resolve id', async () => {
    return createAndResolveId(page, api, createData)
  })

  await test.step('Edit', async () => {
    await page.expectRowVisibleById(rowId)
    await page.editById(rowId, editPatch)
  })

  await test.step('Delete', async () => {
    await page.expectRowVisibleById(rowId)
    await page.deleteById(rowId)
    await page.expectRowNotVisibleById(rowId)
  })

  await test.step('API verify: no resources with prefix', assertNoPrefix)
}
