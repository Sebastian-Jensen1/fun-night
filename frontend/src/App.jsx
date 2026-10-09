import { Routes, Route, Navigate } from 'react-router-dom'
import LawnBackground from './components/LawnBackground'
import Home from './pages/Home'
import QuickPlay from './pages/QuickPlay'
import Setup from './pages/Setup'
import Play from './pages/Play'

function App() {
  return (
    <LawnBackground>
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/quickplay" element={<QuickPlay />} />
        <Route path="/setup/full" element={<Setup mode="FULL" />} />
        <Route path="/setup/quick/:gameId" element={<Setup mode="QUICKPLAY" />} />
        <Route path="/play/:sessionId" element={<Play />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </LawnBackground>
  )
}

export default App
