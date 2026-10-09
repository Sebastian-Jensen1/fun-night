import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { api } from '../api/client'
import { lastPlayers, tokenStore } from '../api/storage'
import { cleanName, MAX_NAME_LENGTH, MAX_PLAYERS, MIN_PLAYERS, validateNames } from '../lib/players'
import GameIcon from '../components/GameIcon'
import { useGames } from '../hooks/useGames'
import './Setup.css'

function initialNames() {
  const saved = lastPlayers.get()
  return saved.length >= MIN_PLAYERS ? saved : ['', '']
}

/** Pick how many players and what they're called, then start the session. */
function Setup({ mode }) {
  const navigate = useNavigate()
  const { gameId } = useParams()
  const { games } = useGames()
  const [names, setNames] = useState(initialNames)
  const [touched, setTouched] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState(null)

  const game = mode === 'QUICKPLAY' ? games?.find((g) => g.id === gameId) : null
  const errors = validateNames(names)
  const valid = errors.every((e) => e === null)

  const setCount = (count) => {
    setNames((prev) => Array.from({ length: count }, (_, i) => prev[i] ?? ''))
  }

  const setName = (index, value) => {
    setNames((prev) => prev.map((n, i) => (i === index ? value : n)))
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setTouched(true)
    if (!valid || submitting) return
    setSubmitting(true)
    setError(null)
    try {
      const playerNames = names.map(cleanName)
      const { session, token } = await api.createSession({ mode, gameId: mode === 'QUICKPLAY' ? gameId : null, playerNames })
      tokenStore.set(session.id, token)
      lastPlayers.set(playerNames)
      navigate(`/play/${session.id}`, { replace: true })
    } catch (err) {
      setError(err.message)
      setSubmitting(false)
    }
  }

  const title = mode === 'FULL' ? 'Full Night' : game?.name ?? 'Quick Play'

  return (
    <div className="page">
      <header className="wood-sign setup__sign">
        {mode === 'QUICKPLAY' && <GameIcon gameId={gameId} size={56} />}
        <div>
          <h1 className="outlined page-title">{title}</h1>
          <p className="sign-sub">{mode === 'FULL' ? 'Six games. One champion. Many sips.' : 'Who is playing?'}</p>
        </div>
      </header>

      {games && mode === 'QUICKPLAY' && (!game || !game.available) && (
        <p className="error-banner" role="alert">That game isn't ready yet. Pick another one!</p>
      )}

      <form className="panel setup" onSubmit={handleSubmit} noValidate>
        <fieldset className="setup__count">
          <legend className="display">How many players?</legend>
          <div className="setup__count-buttons" role="radiogroup">
            {Array.from({ length: MAX_PLAYERS - MIN_PLAYERS + 1 }, (_, i) => i + MIN_PLAYERS).map((n) => (
              <button
                key={n}
                type="button"
                role="radio"
                aria-checked={names.length === n}
                className={`sun-token ${names.length === n ? 'sun-token--active' : ''}`}
                onClick={() => setCount(n)}
              >
                {n}
              </button>
            ))}
          </div>
        </fieldset>

        <fieldset className="setup__names">
          <legend className="display">Player names</legend>
          <ol className="setup__name-list">
            {names.map((name, i) => {
              const showError = touched && errors[i]
              return (
                <li key={i} className="setup__name-row">
                  <label className="setup__label" htmlFor={`player-${i}`}>
                    <span className="setup__badge">{i + 1}</span>
                    <span className="sr-only">Player {i + 1} name</span>
                  </label>
                  <input
                    id={`player-${i}`}
                    className={`setup__input ${showError ? 'setup__input--error' : ''}`}
                    value={name}
                    maxLength={MAX_NAME_LENGTH}
                    autoComplete="off"
                    placeholder={`Player ${i + 1}`}
                    onChange={(e) => setName(i, e.target.value)}
                    aria-invalid={Boolean(showError)}
                    aria-describedby={showError ? `player-${i}-error` : undefined}
                  />
                  {showError && (
                    <span id={`player-${i}-error`} className="setup__error">
                      {errors[i]}
                    </span>
                  )}
                </li>
              )
            })}
          </ol>
        </fieldset>

        {error && <p className="error-banner" role="alert">{error}</p>}

        <div className="setup__actions">
          <Link to={mode === 'FULL' ? '/' : '/quickplay'} className="btn btn--wood">◀ Back</Link>
          <button type="submit" className="btn btn--green btn--big" disabled={submitting || (touched && !valid)}>
            {submitting ? 'Planting…' : "Let's go!"}
          </button>
        </div>
      </form>
    </div>
  )
}

export default Setup
