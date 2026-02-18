import { useTranslation } from 'react-i18next'

export function HelpRoute() {
  const { t } = useTranslation()

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-semibold">{t('helpPage.title')}</h1>
      <div className="rounded-lg border border-slate-200 bg-white p-4 text-sm text-slate-700">
        {t('helpPage.text')}
      </div>
    </div>
  )
}
