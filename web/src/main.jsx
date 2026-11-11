import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import '@fontsource/orbitron/latin-700.css'
import '@fontsource/orbitron/latin-900.css'
import '@fontsource/exo-2/latin-500.css'
import '@fontsource/exo-2/latin-700.css'
import '@fontsource/mr-dafoe/latin-400.css'
import '@fontsource/press-start-2p/latin-400.css'
import '@fontsource/michroma/latin-400.css'
import '@fontsource/outfit/latin-400.css'
import '@fontsource/outfit/latin-600.css'
import '@fontsource/outfit/latin-800.css'
import './styles/base.css'
import './styles/games.css'
import './styles/themes/synthwave.css'
import './styles/themes/pixel.css'
import './styles/themes/y2k.css'
import { ThemeProvider } from './lib/theme'
import App from './App.jsx'

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <ThemeProvider>
      <BrowserRouter>
        <App />
      </BrowserRouter>
    </ThemeProvider>
  </StrictMode>,
)
