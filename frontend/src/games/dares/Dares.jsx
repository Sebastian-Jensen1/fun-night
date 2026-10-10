import { useEffect, useState } from 'react'
import { api } from '../../api/client'
import Zombie from '../../components/Zombie'
import './Dares.css'

/** Brain Dares: take turns drawing a dare. Do it, or chicken out and drink. */
export default function Dares({ sessionId, token, onScoresChanged }) {
  const [state, setState] = useState(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState(null)

  useEffect(() => {
    let cancelled = false
    api.dares
      .get(sessionId, token)
      .then((s) => !cancelled && setState(s))
      .catch((err) => !cancelled && setError(err.message))
    return () => {
      cancelled = true
    }
  }, [sessionId, token])

  const run = async (call, scoresChange = false) => {
    setBusy(true)
    setError(null)
    try {
      setState(await call())
      if (scoresChange) onScoresChanged()
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  if (!state) {
    return error ? <p className="error-banner" role="alert">{error}</p> : <p className="panel">Thinking of something evil…</p>
  }

  const { phase, currentPlayerId, currentPlayerName, currentDare, chickenSips, completedSipsToGive, lastResult } = state
  const drawing = phase === 'DRAW'

  return (
    <div className="dares">
      <div className="dares__host">
        <div className="dares__zombie">
          <Zombie cheering={!drawing} />
        </div>
        <p className="dares__speech" aria-live="polite">
          {drawing ? `${currentPlayerName}… draw a dare. BRAAAINS!` : `Well, ${currentPlayerName}? Do it, or drink ${chickenSips}!`}
        </p>
      </div>

      <div className={`dares__card ${drawing ? 'dares__card--down' : 'pop-in'}`} key={state.turn + phase}>
        {drawing ? (
          <span className="dares__card-back display" aria-hidden="true">?</span>
        ) : (
          <>
            <span className="dares__card-label display">{currentPlayerName}'s dare</span>
            <p className="dares__card-text">{currentDare}</p>
          </>
        )}
      </div>

      {error && <p className="error-banner" role="alert">{error}</p>}

      <div className="dares__controls">
        {drawing ? (
          <button type="button" className="btn btn--big" disabled={busy} onClick={() => run(() => api.dares.draw(sessionId, token, currentPlayerId))}>
            Draw a dare 🧠
          </button>
        ) : (
          <>
            <button
              type="button"
              className="btn btn--green"
              disabled={busy}
              onClick={() => run(() => api.dares.resolve(sessionId, token, currentPlayerId, true), true)}
            >
              Did it! Give {completedSipsToGive}
            </button>
            <button
              type="button"
              className="btn btn--red"
              disabled={busy}
              onClick={() => run(() => api.dares.resolve(sessionId, token, currentPlayerId, false), true)}
            >
              Chicken out 🐔 Drink {chickenSips}
            </button>
          </>
        )}
      </div>

      {lastResult && drawing && (
        <p className="dares__last pop-in" key={state.turn}>
          {lastResult.completed
            ? `${lastResult.playerName} did it and hands out ${lastResult.sipsGiven}!`
            : `${lastResult.playerName} chickened out and drinks ${lastResult.sipsDrunk}. 🐔`}
        </p>
      )}
    </div>
  )
}
