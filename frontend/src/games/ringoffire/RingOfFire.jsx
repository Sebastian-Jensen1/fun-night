import { useEffect, useState } from 'react'
import { api } from '../../api/client'
import PlayingCard from '../../components/PlayingCard'
import './RingOfFire.css'

const RING_SIZE = 52

function KingsCup({ kingsDrawn, kingsTotal, cupSips }) {
  const fill = Math.min(1, cupSips / 9)
  const top = 26 + (1 - fill) * 64
  return (
    <div className="rof-cup" aria-label={`King's Cup: ${kingsDrawn} of ${kingsTotal} kings pulled, ${cupSips} sips in the cup`}>
      <svg viewBox="0 0 100 110" aria-hidden="true">
        <defs>
          <clipPath id="rof-cup-bowl">
            <path d="M18 26 h64 l-6 40 a24 24 0 0 1 -52 0 Z" />
          </clipPath>
        </defs>
        <path d="M18 26 h64 l-6 40 a24 24 0 0 1 -52 0 Z" fill="#fff6d8" />
        <rect className="rof-cup__drink" x="0" y={top} width="100" height="70" fill="#f2a93b" clipPath="url(#rof-cup-bowl)" />
        <path d="M18 26 h64 l-6 40 a24 24 0 0 1 -52 0 Z" fill="none" stroke="var(--ink)" strokeWidth="4" strokeLinejoin="round" />
        <path d="M50 90 v8 M34 102 h32" stroke="var(--ink)" strokeWidth="5" strokeLinecap="round" />
        <path d="M44 90 h12 l-2 -10 h-8 Z" fill="var(--sun)" stroke="var(--ink)" strokeWidth="3" />
        <path d="M24 26 l6 -14 l10 8 l10 -14 l10 14 l10 -8 l6 14" fill="var(--sun)" stroke="var(--ink)" strokeWidth="3" strokeLinejoin="round" />
      </svg>
      <span className="rof-cup__kings" aria-hidden="true">
        {Array.from({ length: kingsTotal }, (_, i) => (
          <span key={i} className={i < kingsDrawn ? 'rof-cup__king--on' : ''}>♚</span>
        ))}
      </span>
      <span className="rof-cup__sips display">{cupSips} sips</span>
    </div>
  )
}

function ResolvePanel({ state, players, busy, onResolve }) {
  const { rule, currentCard, currentPlayerId, currentPlayerName } = state
  const pickable = players.filter((p) => rule.target === 'ANYONE' || p.id !== currentPlayerId || players.length === 1)
  const prompt = rule.title === 'Mate' ? 'Pick your mate' : rule.target === 'OTHER' ? 'Who drinks?' : 'Who lost?'
  return (
    <div className="rof-resolve panel pop-in" role="dialog" aria-labelledby="rof-rule-title">
      <PlayingCard card={currentCard} size="lg" />
      <div className="rof-resolve__body">
        <span className="rof-resolve__who">{currentPlayerName} pulled…</span>
        <h3 id="rof-rule-title" className="rof-resolve__title">{rule.title}</h3>
        <p className="rof-resolve__desc">{rule.description}</p>
        {rule.target === 'NONE' ? (
          <button type="button" className="btn btn--green" disabled={busy} onClick={() => onResolve(null)}>
            Done ✓
          </button>
        ) : (
          <>
            <span className="rof-resolve__prompt display">{prompt}</span>
            <div className="rof-resolve__players">
              {pickable.map((p) => (
                <button key={p.id} type="button" className="btn btn--small btn--stone" disabled={busy} onClick={() => onResolve(p.id)}>
                  {p.name}
                </button>
              ))}
            </div>
          </>
        )}
      </div>
    </div>
  )
}

function describeResult(r) {
  if (!r) return null
  const drinks = r.drinks.map((d) => `${d.playerName} ${d.sips}`).join(', ')
  return `${r.playerName} pulled ${r.card.rank}: ${r.ruleTitle}.${drinks ? ` Drinks: ${drinks}.` : ''}`
}

/** Ring of Fire (King's Cup). The deck is shuffled server-side; the ring is just for show. */
export default function RingOfFire({ sessionId, token, players, onScoresChanged }) {
  const [state, setState] = useState(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState(null)

  useEffect(() => {
    let cancelled = false
    api.ringOfFire
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
    return error ? <p className="error-banner" role="alert">{error}</p> : <p className="panel">Laying out the ring…</p>
  }

  const { phase, currentPlayerId, currentPlayerName, cardsLeft } = state
  const draw = () => run(() => api.ringOfFire.draw(sessionId, token, currentPlayerId))
  const resolve = (targetId) => run(() => api.ringOfFire.resolve(sessionId, token, currentPlayerId, targetId), true)
  const canDraw = phase === 'DRAW' && !busy

  return (
    <div className="rof">
      <p className="rof__banner display" aria-live="polite">
        {phase === 'OVER' ? 'The ring is broken!' : phase === 'DRAW' ? `${currentPlayerName}, pull a card!` : `${currentPlayerName}'s card`}
      </p>

      <div className="rof__ring">
        {Array.from({ length: cardsLeft }, (_, i) => (
          <button
            key={i}
            type="button"
            className="rof__slot"
            style={{ '--angle': `${(i * 360) / RING_SIZE}deg` }}
            onClick={draw}
            disabled={!canDraw}
            tabIndex={i === 0 ? 0 : -1}
            aria-label={i === 0 ? `Pull a card (${cardsLeft} left)` : undefined}
            aria-hidden={i === 0 ? undefined : true}
          />
        ))}
        <div className="rof__center">
          <KingsCup kingsDrawn={state.kingsDrawn} kingsTotal={state.kingsTotal} cupSips={state.cupSips} />
        </div>
      </div>

      {phase === 'RESOLVE' && <ResolvePanel state={state} players={players} busy={busy} onResolve={resolve} />}

      {phase === 'OVER' && (
        <div className="panel rof__over pop-in">
          <p>{describeResult(state.lastResult)}</p>
          <button type="button" className="btn btn--big" disabled={busy} onClick={() => run(() => api.ringOfFire.restart(sessionId, token))}>
            New ring 🔥
          </button>
        </div>
      )}

      {phase === 'DRAW' && state.lastResult && (
        <p className="rof__last pop-in" key={cardsLeft}>{describeResult(state.lastResult)}</p>
      )}

      {error && <p className="error-banner" role="alert">{error}</p>}

      {(state.questionMaster || state.mates.length > 0) && (
        <div className="rof__status">
          {state.questionMaster && <span className="rof__chip">👑 Question Master: {state.questionMaster}</span>}
          {state.mates.map(([a, b]) => (
            <span key={`${a}-${b}`} className="rof__chip">🍻 {a} & {b}</span>
          ))}
        </div>
      )}

      <details className="panel rof__rules">
        <summary className="display">Card rules</summary>
        <dl>
          {state.rules.map((r) => (
            <div key={r.rank} className="rof__rule">
              <dt className="display">{r.rank} · {r.title}</dt>
              <dd>{r.description}</dd>
            </div>
          ))}
        </dl>
      </details>
    </div>
  )
}
