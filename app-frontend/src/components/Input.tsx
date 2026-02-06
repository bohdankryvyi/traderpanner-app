import type { InputHTMLAttributes } from 'react'

export function Input({
  label,
  error,
  className = '',
  ...props
}: InputHTMLAttributes<HTMLInputElement> & { label: string; error?: string }) {
  return (
    <label className="block">
      <div className="mb-1 text-sm font-medium text-slate-700">{label}</div>
      <input
        className={`w-full rounded-md border bg-white px-3 py-2 text-sm outline-none ring-slate-300 focus:ring-2 ${
          error ? 'border-rose-400 focus:ring-rose-200' : 'border-slate-300 focus:ring-slate-200'
        } ${className}`}
        {...props}
      />
      {error ? <div className="mt-1 text-xs text-rose-600">{error}</div> : null}
    </label>
  )
}

