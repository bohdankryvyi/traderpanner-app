import i18n from 'i18next'
import { initReactI18next } from 'react-i18next'

import en from './en.json'
import ua from './ua.json'

const STORAGE_KEY = 'traderplanner_lang'

function getInitialLanguage(): 'en' | 'ua' {
  const saved = localStorage.getItem(STORAGE_KEY)
  if (saved === 'ua' || saved === 'en') return saved
  return 'en'
}

void i18n.use(initReactI18next).init({
  resources: {
    en: { translation: en },
    ua: { translation: ua },
  },
  lng: getInitialLanguage(),
  fallbackLng: 'en',
  interpolation: { escapeValue: false },
})

export function setLanguage(lang: 'en' | 'ua') {
  localStorage.setItem(STORAGE_KEY, lang)
  void i18n.changeLanguage(lang)
}

export default i18n
