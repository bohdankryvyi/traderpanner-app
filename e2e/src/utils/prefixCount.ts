/**
 * Count items whose note field starts with prefix. Used by negative tests to assert no new record created.
 */
export async function getCountWithPrefix<T>(
  getListResponse: () => Promise<{ body: T[] }>,
  prefix: string,
  getNote: (item: T) => string
): Promise<number> {
  const { body } = await getListResponse()
  return body.filter((item) => (getNote(item) ?? '').startsWith(prefix)).length
}
