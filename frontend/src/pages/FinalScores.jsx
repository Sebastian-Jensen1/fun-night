import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api/client'
import { tokenStore } from '../api/storage'
import Zombie from '../components/Zombie'
import './FinalScores.css'

/** End-of-night screen with awards. Offers a rematch with the same crew. */
export default function FinalScores({ session, token }) {
  const navigate = useNavigate()
  const [error, setError] = useState(null)
  const ranked = [...session.players].sort((a, b) => b.sipsDrunk - a.sipsDrunk || a.name.localeCompare(b.name))
  const mostGiven = [...session.players].sort((a, b) => b.sipsGiven - a.sipsGiven)[0]
  const totalSips = session.players.reduce((sum, p) => sum + p.sipsDrunk, 0)

  const leave = () => {
    api.endSession(session.id, token).catch(() => {})
    tokenStore.remove(session.id)
  }

  const rematch = async () => {
    try {
      const gameId = session.mode === 'QUICKPLAY' ? session.games[0].id : null
      const created = await api.createSession({
        mode: session.mode,
        gameId,
        playerNames: session.players.map((p) => p.name),
      })
      leave()
      tokenStore.set(created.session.id, created.token)
      navigate(`/play/${created.session.id}`, { replace: true })
    } catch (err) {
      setError(err.message)
    }
  }

  const menu = () => {
    leave()
    navigate('/')
  }

  return (
    <div className="page final">
      <header className="wood-sign">
        <h1 className="outlined page-title">The Night Is Over!</h1>
        <p className="sign-sub">{totalSips} sips were sipped</p>
      </header>

      <div className="final__layout">
        <div className="final__zombie">
          <Zombie cheering />
        </div>
        <ol className="panel final__list">
          {ranked.map((p, i) => (
            <li key={p.id} className="final__row pop-in" style={{ animationDelay: `${i * 0.08}s` }}>
              <span className="final__rank display">{i + 1}</span>
              <span className="final__name">
                {p.name}
                {i === 0 && p.sipsDrunk > 0 && <span className="final__award">🧟 Zombie of the Night</span>}
                {mostGiven && p.id === mostGiven.id && p.sipsGiven > 0 && (
                  <span className="final__award final__award--give">🌻 Garden Defender</span>
                )}
              </span>
              <span className="final__stat">🍺 {p.sipsDrunk}</span>
              <span className="final__stat">👉 {p.sipsGiven}</span>
            </li>
          ))}
        </ol>
      </div>

      {error && <p className="error-banner" role="alert">{error}</p>}

      <div className="final__actions">
        <button type="button" className="btn btn--wood" onClick={menu}>Main menu</button>
        <button type="button" className="btn btn--green btn--big" onClick={rematch}>Rematch!</button>
      </div>
    </div>
  )
}
