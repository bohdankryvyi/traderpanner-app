import { Link, NavLink } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { setLanguage } from '../i18n'
import { Button } from './Button'

export function Header() {
  const { t, i18n } = useTranslation()
  const lang = (i18n.language === 'ua' ? 'ua' : 'en') as 'en' | 'ua'

  return (
    <header className="border-b border-slate-200 bg-white">
      <div className="mx-auto flex max-w-6xl items-center justify-between px-4 py-3">
        <Link to="/" className="text-lg font-semibold text-slate-900">
          {t('appName')}
        </Link>

        <div className="flex items-center gap-3">
          <div className="hidden text-sm text-slate-600 sm:block">{t('header.dataStatus')}</div>

          <div className="flex items-center gap-2">
            <Button
              type="button"
              variant={lang === 'en' ? 'primary' : 'secondary'}
              className="px-3 py-1.5"
              onClick={() => setLanguage('en')}
            >
              {t('header.langEn')}
            </Button>
            <Button
              type="button"
              variant={lang === 'ua' ? 'primary' : 'secondary'}
              className="px-3 py-1.5"
              onClick={() => setLanguage('ua')}
            >
              {t('header.langUa')}
            </Button>
          </div>

          <NavLink
            to="/help"
            className={({ isActive }) =>
              `text-sm font-medium ${isActive ? 'text-slate-900' : 'text-slate-600 hover:text-slate-900'}`
            }
          >
            {t('header.help')}
          </NavLink>
        </div>
      </div>
    </header>
  )
}

