/**
 * Returns the number of items whose notes/note field starts with prefix.
 * Used by negative tests to assert no new record was created.
 */
export async function countByPrefix<T>(
  getList: () => Promise<{ body: T[] }>,
  prefix: string,
  getNotesField: (item: T) => string
): Promise<number> {
  const { body } = await getList()
  return body.filter((item) => (getNotesField(item) ?? '').startsWith(prefix)).length
}
