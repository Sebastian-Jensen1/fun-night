import { useCallback, useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { api } from '../api/client'
import { tokenStore } from '../api/storage'
import Scoreboard from '../components/Scoreboard'
import GameIcon from '../components/GameIcon'
import { GAME_COMPONENTS } from '../games/registry'
import FinalScores from './FinalScores'
import './Play.css'

function ProgressTrail({ games, currentIndex }) {
  return (
    <ol className="trail" aria-label="Game progress">
      {games.map((g, i) => (
        <li key={g.id} className={`trail__step trail__step--${g.status.toLowerCase()}`}>
          <span className="trail__icon">
            <GameIcon gameId={g.available ? g.id : 'mystery'} size={34} />
          </span>
          <span className="trail__label">{g.name}</span>
          {i === currentIndex && g.status === 'ACTIVE' && <span className="sr-only"> (now playing)</span>}
          {g.status === 'SKIPPED' && <span className="sr-only"> (coming soon, skipped)</span>}
        </li>
      ))}
    </ol>
  )
}

function Play() {
  const { sessionId } = useParams()
  const navigate = useNavigate()
  const [token] = useState(() => tokenStore.get(sessionId))
  const [session, setSession] = useState(null)
  const [error, setError] = useState(token ? null : 'missing-token')
  const [busy, setBusy] = useState(false)

  const refresh = useCallback(() => {
    if (!token) return Promise.resolve()
    return api
      .getSession(sessionId, token)
      .then(setSession)
      .catch((err) => setError(err.status === 404 || err.status === 403 ? 'missing-token' : err.message))
  }, [sessionId, token])

  useEffect(() => {
    refresh()
  }, [refresh])

  const advance = async () => {
    setBusy(true)
    try {
      setSession(await api.nextGame(sessionId, token))
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  const quit = async () => {
    if (!window.confirm('Leave this game night? Scores will be lost.')) return
    try {
      await api.endSession(sessionId, token)
    } catch {
      // already gone; nothing to clean up
    }
    tokenStore.remove(sessionId)
    navigate('/')
  }

  if (error === 'missing-token') {
    return (
      <div className="page">
        <div className="panel play__message">
          <h1 className="display">This game night has ended</h1>
          <p>It may have expired, or it was started in another browser tab.</p>
          <Link to="/" className="btn btn--green">Back to the menu</Link>
        </div>
      </div>
    )
  }

  if (!session) {
    return (
      <div className="page">
        {error ? <p className="error-banner" role="alert">{error}</p> : <p className="panel">Setting up the table…</p>}
      </div>
    )
  }

  if (session.finished) {
    return <FinalScores session={session} token={token} />
  }

  const current = session.currentGame
  const GameComponent = GAME_COMPONENTS[current.id]
  const remaining = session.games.slice(session.currentGameIndex + 1).filter((g) => g.available)
  const isLast = remaining.length === 0

  return (
    <div className="page play">
      <div className="play__topbar">
        <button type="button" className="btn btn--wood btn--small" onClick={quit}>✕ Quit</button>
        {session.mode === 'FULL' ? (
          <ProgressTrail games={session.games} currentIndex={session.currentGameIndex} />
        ) : (
          <span className="play__mode display">Quick Play</span>
        )}
      </div>

      {error && <p className="error-banner" role="alert">{error}</p>}

      <section className="play__stage" aria-label={current.name}>
        {GameComponent ? (
          <GameComponent
            key={current.id}
            sessionId={sessionId}
            token={token}
            players={session.players}
            onScoresChanged={refresh}
          />
        ) : (
          <div className="panel">This game is still growing. Skip ahead!</div>
        )}
      </section>

      <Scoreboard players={session.players} />

      <div className="play__next">
        <button type="button" className="btn btn--green btn--big" onClick={advance} disabled={busy}>
          {isLast ? 'Finish & see scores 🏆' : `Next game: ${remaining[0].name} ▶`}
        </button>
      </div>
    </div>
  )
}

export default Play
