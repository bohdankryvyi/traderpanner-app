import type { SelectHTMLAttributes } from 'react'

export function Select({
  label,
  error,
  className = '',
  children,
  ...props
}: SelectHTMLAttributes<HTMLSelectElement> & { label: string; error?: string }) {
  return (
    <label className="block">
      <div className="mb-1 text-sm font-medium text-slate-700">{label}</div>
      <select
        className={`w-full rounded-md border bg-white px-3 py-2 text-sm outline-none ring-slate-300 focus:ring-2 ${
          error ? 'border-rose-400 focus:ring-rose-200' : 'border-slate-300 focus:ring-slate-200'
        } ${className}`}
        {...props}
      >
        {children}
      </select>
      {error ? <div className="mt-1 text-xs text-rose-600">{error}</div> : null}
    </label>
  )
}
