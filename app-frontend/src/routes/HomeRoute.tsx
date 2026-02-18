import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'
import { Button } from '../components/Button'

export function HomeRoute() {
  const { t } = useTranslation()

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold">{t('home.title')}</h1>
        <p className="mt-2 max-w-3xl text-slate-700">{t('home.description')}</p>
      </div>

      <div className="rounded-lg border border-slate-200 bg-white p-4">
        <div className="text-sm font-semibold text-slate-900">{t('home.disclaimerTitle')}</div>
        <div className="mt-1 text-sm text-slate-700">{t('home.disclaimerText')}</div>
      </div>

      <div className="flex flex-col gap-3 sm:flex-row">
        <Link to="/trading" className="w-full sm:w-auto">
          <Button className="w-full sm:w-auto">{t('home.goTrading')}</Button>
        </Link>
        <Link to="/portfolio" className="w-full sm:w-auto">
          <Button variant="secondary" className="w-full sm:w-auto">
            {t('home.goPortfolio')}
          </Button>
        </Link>
      </div>
    </div>
  )
}
