import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import '@fontsource/orbitron/latin-700.css'
import '@fontsource/orbitron/latin-900.css'
import '@fontsource/exo-2/latin-500.css'
import '@fontsource/exo-2/latin-700.css'
import '@fontsource/mr-dafoe/latin-400.css'
import '@fontsource/mochiy-pop-one/latin-400.css'
import '@fontsource/zen-maru-gothic/latin-500.css'
import '@fontsource/zen-maru-gothic/latin-700.css'
import '@fontsource/zen-maru-gothic/latin-900.css'
import '@fontsource/luckiest-guy/latin-400.css'
import '@fontsource/nunito/latin-700.css'
import '@fontsource/nunito/latin-900.css'
import '@fontsource/cinzel/latin-700.css'
import '@fontsource/cinzel/latin-900.css'
import '@fontsource/source-sans-3/latin-400.css'
import '@fontsource/source-sans-3/latin-600.css'
import '@fontsource/source-sans-3/latin-700.css'
import './styles/base.css'
import './styles/games.css'
import './styles/themes/synthwave.css'
import './styles/themes/kawaii.css'
import './styles/themes/undersea.css'
import './styles/themes/runic.css'
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
