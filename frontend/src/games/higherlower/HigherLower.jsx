import { useEffect, useState } from 'react'
import { api } from '../../api/client'
import Dealer from '../../components/Dealer'
import PlayingCard from '../../components/PlayingCard'
import './HigherLower.css'

function dealerMood(result) {
  if (!result) return 'idle'
  if (result.passed) return 'shock'
  return result.correct ? 'sad' : 'happy'
}

function dealerLine(state) {
  const r = state.lastResult
  const ask = `${state.currentPlayerName}: higher or lower than ${state.currentCard.rank}?`
  if (!r) return `${ask} Get it wrong and you drink the pile.`
  if (r.passed) return `${r.playerName} passes a pile of ${state.pile} cards! ${ask}`
  if (!r.correct) {
    const same = r.from.rank === r.drawn.rank ? ' Same card counts as wrong!' : ''
    return `Ha! ${r.drawn.rank} it was. ${r.playerName} drinks ${r.sipsDrunk}.${same} ${ask}`
  }
  return `${r.drawn.rank}! Correct… ${ask}`
}

/** Higher or Lower with Dealer Dan. The deck lives on the server so nobody can peek. */
export default function HigherLower({ sessionId, token, onScoresChanged }) {
  const [state, setState] = useState(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState(null)

  useEffect(() => {
    let cancelled = false
    api.higherLower
      .get(sessionId, token)
      .then((s) => !cancelled && setState(s))
      .catch((err) => !cancelled && setError(err.message))
    return () => {
      cancelled = true
    }
  }, [sessionId, token])

  const run = async (call) => {
    setBusy(true)
    setError(null)
    try {
      const next = await call()
      setState(next)
      if (next.lastResult?.sipsDrunk > 0) onScoresChanged()
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  if (!state) {
    return error ? <p className="error-banner" role="alert">{error}</p> : <p className="panel">Shuffling…</p>
  }

  const { currentCard, currentPlayerId, streak, streakToPass, pile, sipsAtStake, canPass, lastResult } = state
  const guess = (g) => run(() => api.higherLower.guess(sessionId, token, currentPlayerId, g))
  const mood = dealerMood(lastResult)
  const turnKey = `${pile}-${currentCard.rank}-${currentCard.suit}`

  return (
    <div className="hl">
      <div className="hl__host">
        <div className="hl__dealer">
          <Dealer mood={mood} />
        </div>
        <p className="hl__speech" aria-live="polite">{dealerLine(state)}</p>
      </div>

      <div className="hl__table">
        <div className="hl__cards">
          {lastResult?.drawn && (
            <div className="hl__previous" aria-label="Previous card">
              <PlayingCard card={lastResult.from} />
            </div>
          )}
          <div className="hl__current" key={turnKey}>
            <PlayingCard card={currentCard} size="lg" />
          </div>
          {lastResult && !lastResult.passed && (
            <span className={`hl__verdict display pop-in ${lastResult.correct ? 'hl__verdict--right' : 'hl__verdict--wrong'}`} key={`v-${turnKey}`}>
              {lastResult.correct ? 'Correct!' : `Drink ${lastResult.sipsDrunk}!`}
            </span>
          )}
        </div>

        <div className="hl__stats">
          <div className="hl__stat">
            <span className="hl__stat-label">Pile</span>
            <span className="hl__pile display">{pile} {pile === 1 ? 'card' : 'cards'}</span>
            <span className="hl__stat-note">Wrong guess = drink {sipsAtStake}</span>
          </div>
          <div className="hl__stat">
            <span className="hl__stat-label">{state.currentPlayerName}'s streak</span>
            <span className="hl__streak" aria-label={`${streak} right in a row`}>
              {Array.from({ length: streakToPass }, (_, i) => (
                <span key={i} className={`hl__sun ${i < streak ? 'hl__sun--lit' : ''}`} aria-hidden="true" />
              ))}
              {streak > streakToPass && <span className="hl__extra">+{streak - streakToPass}</span>}
            </span>
            <span className="hl__stat-note">{canPass ? 'You may pass the pile on!' : `${streakToPass} in a row to pass`}</span>
          </div>
        </div>
      </div>

      {error && <p className="error-banner" role="alert">{error}</p>}

      <div className="hl__controls">
        <button type="button" className="btn btn--green btn--big" disabled={busy} onClick={() => guess('HIGHER')}>
          ▲ Higher
        </button>
        <button type="button" className="btn btn--red btn--big" disabled={busy} onClick={() => guess('LOWER')}>
          ▼ Lower
        </button>
        {canPass && (
          <button
            type="button"
            className="btn pop-in"
            disabled={busy}
            onClick={() => run(() => api.higherLower.pass(sessionId, token, currentPlayerId))}
          >
            Pass the pile ➜
          </button>
        )}
      </div>

      <details className="panel hl__howto">
        <summary className="display">How to play</summary>
        <ul>
          <li>Guess if the next card is higher or lower. Aces are high. The same rank counts as wrong.</li>
          <li>Every right guess adds a card to the pile.</li>
          <li>Wrong guess: drink 1 sip per card in the pile (max {state.maxSips}). The pile resets and the next player goes.</li>
          <li>Get {streakToPass} right in a row and you may pass the pile, and the turn, to the next player.</li>
        </ul>
      </details>
    </div>
  )
}
