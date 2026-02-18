import type { ReactNode } from 'react'

export type ColumnDef<T> = {
  header: string
  render: (row: T) => ReactNode
  className?: string
}

export function Table<T>({
  columns,
  rows,
  rowKey,
  rowTestId,
}: {
  columns: Array<ColumnDef<T>>
  rows: T[]
  rowKey: (row: T) => string
  rowTestId?: (row: T) => string
}) {
  return (
    <div className="overflow-x-auto rounded-lg border border-slate-200 bg-white">
      <table className="w-full border-collapse text-left text-sm">
        <thead className="bg-slate-50 text-xs uppercase tracking-wide text-slate-600">
          <tr>
            {columns.map((c, idx) => (
              <th key={idx} className={`whitespace-nowrap px-4 py-3 ${c.className ?? ''}`}>
                {c.header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr
              key={rowKey(row)}
              className="border-t border-slate-200"
              data-testid={rowTestId?.(row)}
            >
              {columns.map((c, idx) => (
                <td key={idx} className={`whitespace-nowrap px-4 py-3 ${c.className ?? ''}`}>
                  {c.render(row)}
                </td>
              ))}
            </tr>
          ))}
          {rows.length === 0 ? (
            <tr>
              <td className="px-4 py-6 text-center text-slate-500" colSpan={columns.length}>
                —
              </td>
            </tr>
          ) : null}
        </tbody>
      </table>
    </div>
  )
}
