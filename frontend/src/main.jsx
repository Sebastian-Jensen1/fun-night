import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom' //This does so React checks the browser address
import './index.css'
import App from './App.jsx'

createRoot(document.getElementById('root')).render(
  <StrictMode>
      <BrowserRouter>
          <App />
      </BrowserRouter>
  </StrictMode>,
)
