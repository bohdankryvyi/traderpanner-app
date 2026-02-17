import { useEffect, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import axios from 'axios'
import { api } from '../api/endpoints'
import { getApiErrorMessage } from '../api/apiClient'
import type { TradingEntryRequest, TradingEntryResponse } from '../api/types'
import { Button } from '../components/Button'
import { Input } from '../components/Input'
import { Select } from '../components/Select'
import { Table, type ColumnDef } from '../components/Table'

type FormState = {
  ticker: string
  note: string
  entryPrice: string
}

function formatDateTime(iso: string) {
  const d = new Date(iso)
  // keep simple and locale-friendly for MVP
  return isNaN(d.getTime()) ? iso : d.toLocaleString()
}

export function TradingRoute() {
  const { t } = useTranslation()

  const [rows, setRows] = useState<TradingEntryResponse[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const [editingId, setEditingId] = useState<number | null>(null)
  const [form, setForm] = useState<FormState>({
    ticker: '',
    note: '',
    entryPrice: '',
  })
  const [formError, setFormError] = useState<string | null>(null)

  const [aiTimeframe, setAiTimeframe] = useState<'1h' | '1d'>('1h')
  const [aiLoading, setAiLoading] = useState(false)
  const [aiError, setAiError] = useState<string | null>(null)
  const [aiPatternResult, setAiPatternResult] = useState<{ ticker: string; pattern: string; rationale: string; generatedAt: string; source?: string } | null>(null)

  async function load() {
    setLoading(true)
    setError(null)
    try {
      const data = await api.trading.list()
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
    setForm({ ticker: '', note: '', entryPrice: '' })
    setFormError(null)
  }

  async function onSubmit() {
    setFormError(null)
    const ticker = form.ticker.trim().toUpperCase()
    const entryPriceStr = form.entryPrice.trim()
    const entryPrice = entryPriceStr === '' ? null : Number(entryPriceStr)
    if (!ticker) return setFormError(`${t('trading.form.ticker')} is required`)
    if (entryPrice !== null && (Number.isNaN(entryPrice) || entryPrice <= 0)) {
      return setFormError(`${t('trading.form.entryPrice')} must be a positive number when provided`)
    }

    const payload: TradingEntryRequest = {
      ticker,
      note: form.note.trim() ? form.note.trim() : null,
      entryPrice: entryPrice ?? undefined,
    }

    try {
      if (editingId == null) {
        await api.trading.create(payload)
      } else {
        await api.trading.update(editingId, payload)
      }
      resetForm()
      await load()
    } catch (e) {
      setFormError(getApiErrorMessage(e))
    }
  }

  async function onDelete(id: number) {
    try {
      await api.trading.delete(id)
      if (id === editingId) {
        resetForm()
      }
      await load()
    } catch (e) {
      setError(getApiErrorMessage(e))
    }
  }

  function onEdit(row: TradingEntryResponse) {
    setEditingId(row.id)
    setForm({
      ticker: row.ticker,
      note: row.note ?? '',
      entryPrice: row.entryPrice != null ? String(row.entryPrice) : '',
    })
    setFormError(null)
  }

  async function analyzeNow() {
    setAiLoading(true)
    setAiError(null)
    setAiPatternResult(null)
    try {
      const res = await api.ai.pattern(aiTimeframe)
      setAiPatternResult({ ticker: res.ticker, pattern: res.pattern, rationale: res.rationale ?? '', generatedAt: res.generatedAt, source: res.source })
    } catch (e: unknown) {
      const msg = getApiErrorMessage(e)
      const status = axios.isAxiosError(e) ? e.response?.status : undefined
      if (msg.toUpperCase().includes('OPENAI_API_KEY')) {
        setAiError(t('trading.ai.missingKey'))
      } else if (status === 502) {
        setAiError(t('trading.ai.unavailable') + ' ' + msg)
      } else {
        setAiError(msg)
      }
    } finally {
      setAiLoading(false)
    }
  }

  const columns: Array<ColumnDef<TradingEntryResponse>> = useMemo(
    () => [
      { header: t('trading.table.ticker'), render: (r) => r.ticker },
      { header: t('trading.table.note'), render: (r) => r.note ?? '—' },
      { header: t('trading.table.entryPrice'), render: (r) => (r.entryPrice != null ? r.entryPrice : '—') },
      { header: t('trading.table.currentPriceUsd'), render: (r) => r.currentPriceUsd },
      {
        header: t('trading.table.actions'),
        className: 'text-right',
        render: (r) => (
          <div className="flex justify-end gap-2">
            <Button type="button" variant="secondary" className="px-3 py-1.5" data-testid="trading-row-edit" onClick={() => onEdit(r)}>
              {t('common.edit')}
            </Button>
            <Button type="button" variant="danger" className="px-3 py-1.5" data-testid="trading-row-delete" onClick={() => onDelete(r.id)}>
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
        <h1 className="text-2xl font-semibold">{t('trading.title')}</h1>
        <Button type="button" variant="secondary" onClick={load} disabled={loading}>
          {t('common.refreshPrices')}
        </Button>
      </div>

      <div className="rounded-lg border border-slate-200 bg-white p-4">
        <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
          <Input
            data-testid="trading-ticker"
            label={t('trading.form.ticker')}
            value={form.ticker}
            onChange={(e) => setForm((s) => ({ ...s, ticker: e.target.value }))}
            placeholder="AAPL"
          />
          <Input
            data-testid="trading-note"
            label={t('trading.form.note')}
            value={form.note}
            onChange={(e) => setForm((s) => ({ ...s, note: e.target.value }))}
            placeholder="Optional"
          />
          <Input
            data-testid="trading-entry-price"
            label={t('trading.form.entryPrice')}
            type="number"
            inputMode="decimal"
            value={form.entryPrice}
            onChange={(e) => setForm((s) => ({ ...s, entryPrice: e.target.value }))}
            placeholder="Optional"
          />
        </div>

        {formError ? <div className="mt-3 text-sm text-rose-700" data-testid="trading-form-error">{formError}</div> : null}

        <div className="mt-4 flex gap-2">
          <Button type="button" data-testid="trading-form-submit" onClick={onSubmit}>
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

      <Table columns={columns} rows={rows} rowKey={(r) => String(r.id)} rowTestId={(r) => `trading-row-${r.id}`} />

      <div className="rounded-lg border border-slate-200 bg-white p-4">
        <div className="mb-3 text-base font-semibold">{t('trading.ai.title')}</div>
        <div className="flex flex-col gap-3 sm:flex-row sm:items-end">
          <Select
            label={t('trading.ai.timeframe')}
            value={aiTimeframe}
            onChange={(e) => setAiTimeframe(e.target.value as '1h' | '1d')}
            className="sm:w-40"
          >
            <option value="1h">1h</option>
            <option value="1d">1d</option>
          </Select>
          <Button type="button" onClick={analyzeNow} disabled={aiLoading}>
            {t('trading.ai.analyzeNow')}
          </Button>
        </div>

        {aiError ? <div className="mt-3 text-sm text-rose-700">{aiError}</div> : null}

        {aiPatternResult ? (
          <div className="mt-4 rounded-md bg-slate-50 p-3 text-sm text-slate-800">
            <div className="flex items-center gap-2 flex-wrap">
              <span className="font-medium">
                {t('trading.ai.bestSetup')}: {aiPatternResult.ticker} — {aiPatternResult.pattern}
              </span>
              {aiPatternResult.source?.startsWith('fallback') ? (
                <span className="rounded bg-amber-100 px-1.5 py-0.5 text-xs text-amber-800" title={aiPatternResult.source}>
                  AI fallback
                </span>
              ) : null}
            </div>
            {aiPatternResult.rationale ? (
              <p className="mt-2 text-slate-700">{aiPatternResult.rationale}</p>
            ) : null}
            <div className="mt-2 text-xs text-slate-600">
              {t('trading.ai.generatedAt')}: {formatDateTime(aiPatternResult.generatedAt)}
            </div>
          </div>
        ) : null}
      </div>
    </div>
  )
}

