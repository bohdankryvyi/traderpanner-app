import { useEffect, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { api } from '../api/endpoints'
import { getApiErrorMessage } from '../api/apiClient'
import type { Currency, PortfolioPositionRequest, PortfolioPositionResponse } from '../api/types'
import { Button } from '../components/Button'
import { Input } from '../components/Input'
import { Select } from '../components/Select'
import { Table, type ColumnDef } from '../components/Table'

type FormState = {
  sector: string
  company: string
  ticker: string
  buyPrice: string
  targetPrice: string
  quantity: string
  currency: Currency
  notes: string
}

function num(v: number | null | undefined) {
  if (v == null) return '—'
  return Number(v).toFixed(4)
}

export function PortfolioRoute() {
  const { t } = useTranslation()

  const [rows, setRows] = useState<PortfolioPositionResponse[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const [editingId, setEditingId] = useState<number | null>(null)
  const [form, setForm] = useState<FormState>({
    sector: '',
    company: '',
    ticker: '',
    buyPrice: '',
    targetPrice: '',
    quantity: '',
    currency: 'USD',
    notes: '',
  })
  const [formError, setFormError] = useState<string | null>(null)

  async function load() {
    setLoading(true)
    setError(null)
    try {
      const data = await api.portfolio.list()
      setRows(data)
    } catch (e) {
      setError(getApiErrorMessage(e))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void load()
  }, [])

  function resetForm() {
    setEditingId(null)
    setForm({
      sector: '',
      company: '',
      ticker: '',
      buyPrice: '',
      targetPrice: '',
      quantity: '',
      currency: 'USD',
      notes: '',
    })
    setFormError(null)
  }

  async function onSubmit() {
    setFormError(null)

    const sector = form.sector.trim()
    const company = form.company.trim()
    const ticker = form.ticker.trim().toUpperCase()

    const buyPrice = Number(form.buyPrice)
    const quantity = Number(form.quantity)
    const targetPrice = form.targetPrice.trim() ? Number(form.targetPrice) : null

    if (!sector) return setFormError(`${t('portfolio.form.sector')} is required`)
    if (!company) return setFormError(`${t('portfolio.form.company')} is required`)
    if (!ticker) return setFormError(`${t('portfolio.form.ticker')} is required`)
    if (!form.buyPrice || Number.isNaN(buyPrice) || buyPrice <= 0) {
      return setFormError(`${t('portfolio.form.buyPrice')} must be a positive number`)
    }
    if (!form.quantity || Number.isNaN(quantity) || quantity <= 0) {
      return setFormError(`${t('portfolio.form.quantity')} must be a positive number`)
    }
    if (targetPrice != null && (Number.isNaN(targetPrice) || targetPrice <= 0)) {
      return setFormError(`${t('portfolio.form.targetPrice')} must be a positive number`)
    }

    const payload: PortfolioPositionRequest = {
      sector,
      company,
      ticker,
      buyPrice,
      targetPrice,
      quantity,
      currency: form.currency,
      notes: form.notes.trim() ? form.notes.trim() : null,
    }

    try {
      if (editingId == null) {
        await api.portfolio.create(payload)
      } else {
        await api.portfolio.update(editingId, payload)
      }
      resetForm()
      await load()
    } catch (e) {
      setFormError(getApiErrorMessage(e))
    }
  }

  async function onDelete(id: number) {
    try {
      await api.portfolio.delete(id)
      if (id === editingId) {
        resetForm()
      }
      await load()
    } catch (e) {
      setError(getApiErrorMessage(e))
    }
  }

  function onEdit(row: PortfolioPositionResponse) {
    setEditingId(row.id)
    setForm({
      sector: row.sector,
      company: row.company,
      ticker: row.ticker,
      buyPrice: String(row.buyPrice ?? ''),
      targetPrice: row.targetPrice == null ? '' : String(row.targetPrice),
      quantity: String(row.quantity ?? ''),
      currency: row.currency,
      notes: row.notes ?? '',
    })
    setFormError(null)
  }

  const columns: Array<ColumnDef<PortfolioPositionResponse>> = useMemo(
    () => [
      { header: t('portfolio.table.sector'), render: (r) => r.sector },
      { header: t('portfolio.table.company'), render: (r) => r.company },
      { header: t('portfolio.table.ticker'), render: (r) => r.ticker },
      { header: t('portfolio.table.currency'), render: (r) => r.currency },
      { header: t('portfolio.table.buyPrice'), render: (r) => r.buyPrice },
      { header: t('portfolio.table.targetPrice'), render: (r) => (r.targetPrice == null ? '—' : r.targetPrice) },
      { header: t('portfolio.table.quantity'), render: (r) => r.quantity },
      { header: t('portfolio.table.notes'), render: (r) => r.notes ?? '—' },
      { header: t('portfolio.table.currentPriceUsd'), render: (r) => num(r.currentPriceUsd) },
      { header: t('portfolio.table.upsidePercent'), render: (r) => num(r.upsidePercent) },
      { header: t('portfolio.table.investedAmountUsd'), render: (r) => num(r.investedAmountUsd) },
      { header: t('portfolio.table.sharePercent'), render: (r) => num(r.sharePercent) },
      { header: t('portfolio.table.expectedProfitUsd'), render: (r) => num(r.expectedProfitUsd) },
      { header: t('portfolio.table.profitSharePercent'), render: (r) => num(r.profitSharePercent) },
      {
        header: t('portfolio.table.actions'),
        className: 'text-right',
        render: (r) => (
          <div className="flex justify-end gap-2">
            <Button type="button" variant="secondary" className="px-3 py-1.5" data-testid="portfolio-row-edit" onClick={() => onEdit(r)}>
              {t('common.edit')}
            </Button>
            <Button type="button" variant="danger" className="px-3 py-1.5" data-testid="portfolio-row-delete" onClick={() => onDelete(r.id)}>
              {t('common.delete')}
            </Button>
          </div>
        ),
      },
    ],
    [t],
  )

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between gap-3">
        <h1 className="text-2xl font-semibold">{t('portfolio.title')}</h1>
        <Button type="button" variant="secondary" onClick={load} disabled={loading}>
          {t('common.refreshPrices')}
        </Button>
      </div>

      <div className="rounded-lg border border-slate-200 bg-white p-4">
        <div className="grid grid-cols-1 gap-4 md:grid-cols-4">
          <Input
            data-testid="portfolio-sector"
            label={t('portfolio.form.sector')}
            value={form.sector}
            onChange={(e) => setForm((s) => ({ ...s, sector: e.target.value }))}
          />
          <Input
            data-testid="portfolio-company"
            label={t('portfolio.form.company')}
            value={form.company}
            onChange={(e) => setForm((s) => ({ ...s, company: e.target.value }))}
          />
          <Input
            data-testid="portfolio-ticker"
            label={t('portfolio.form.ticker')}
            value={form.ticker}
            onChange={(e) => setForm((s) => ({ ...s, ticker: e.target.value }))}
            placeholder="AAPL"
          />
          <Select
            label={t('portfolio.form.currency')}
            value={form.currency}
            onChange={(e) => setForm((s) => ({ ...s, currency: e.target.value as Currency }))}
          >
            <option value="USD">USD</option>
            <option value="EUR">EUR</option>
          </Select>

          <Input
            data-testid="portfolio-buy-price"
            label={t('portfolio.form.buyPrice')}
            type="number"
            inputMode="decimal"
            value={form.buyPrice}
            onChange={(e) => setForm((s) => ({ ...s, buyPrice: e.target.value }))}
          />
          <Input
            data-testid="portfolio-target-price"
            label={t('portfolio.form.targetPrice')}
            type="number"
            inputMode="decimal"
            value={form.targetPrice}
            onChange={(e) => setForm((s) => ({ ...s, targetPrice: e.target.value }))}
          />
          <Input
            data-testid="portfolio-quantity"
            label={t('portfolio.form.quantity')}
            type="number"
            inputMode="decimal"
            value={form.quantity}
            onChange={(e) => setForm((s) => ({ ...s, quantity: e.target.value }))}
          />
          <Input
            data-testid="portfolio-notes"
            label={t('portfolio.form.notes')}
            value={form.notes}
            onChange={(e) => setForm((s) => ({ ...s, notes: e.target.value }))}
            placeholder="Optional"
          />
        </div>

        {formError ? <div className="mt-3 text-sm text-rose-700" data-testid="portfolio-form-error">{formError}</div> : null}

        <div className="mt-4 flex gap-2">
          <Button type="button" data-testid="portfolio-form-submit" onClick={onSubmit}>
            {editingId == null ? t('common.create') : t('common.update')}
          </Button>
          {editingId != null ? (
            <Button type="button" variant="secondary" onClick={resetForm}>
              {t('common.cancel')}
            </Button>
          ) : null}
        </div>
      </div>

      {error ? (
        <div className="rounded-lg border border-rose-200 bg-rose-50 p-3 text-sm text-rose-800">
          <span className="font-semibold">{t('common.error')}:</span> {error}
        </div>
      ) : null}

      {loading ? <div className="text-sm text-slate-600">{t('common.loading')}</div> : null}

      <Table columns={columns} rows={rows} rowKey={(r) => String(r.id)} rowTestId={(r) => `portfolio-row-${r.id}`} />
    </div>
  )
}

