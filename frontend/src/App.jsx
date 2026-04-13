import './App.css'
import Home from './pages/Home'
import Session from './pages/Session'
import { Routes, Route } from 'react-router-dom'


function App() {
  return (
    <div className="App">
      <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/create-session/:id" element={<Session/>} />
      </Routes>

    </div>
  )
}

export default App
