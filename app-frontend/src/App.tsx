import { Navigate, Route, Routes } from 'react-router-dom'
import { Header } from './components/Header'
import { PageContainer } from './components/PageContainer'
import { HomeRoute } from './routes/HomeRoute'
import { TradingRoute } from './routes/TradingRoute'
import { PortfolioRoute } from './routes/PortfolioRoute'
import { HelpRoute } from './routes/HelpRoute'

export default function App() {
  return (
    <div className="min-h-screen">
      <Header />
      <PageContainer>
        <Routes>
          <Route path="/" element={<HomeRoute />} />
          <Route path="/trading" element={<TradingRoute />} />
          <Route path="/portfolio" element={<PortfolioRoute />} />
          <Route path="/help" element={<HelpRoute />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </PageContainer>
    </div>
  )
}
