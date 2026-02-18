import type { ButtonHTMLAttributes } from 'react'

type Variant = 'primary' | 'secondary' | 'danger'

export function Button({
  variant = 'primary',
  className = '',
  ...props
}: ButtonHTMLAttributes<HTMLButtonElement> & { variant?: Variant }) {
  const base =
    'inline-flex items-center justify-center rounded-md px-4 py-2 text-sm font-medium transition-colors disabled:cursor-not-allowed disabled:opacity-60'

  const variants: Record<Variant, string> = {
    primary: 'bg-slate-900 text-white hover:bg-slate-800',
    secondary: 'bg-white text-slate-900 ring-1 ring-slate-300 hover:bg-slate-50',
    danger: 'bg-rose-600 text-white hover:bg-rose-500',
  }

  return <button className={`${base} ${variants[variant]} ${className}`} {...props} />
}
