import { useEffect, useState } from 'react'
import { sipChanges } from '../lib/scores'

const SHOW_MS = 3200
let nextId = 1

/**
 * Calls out sips as they land. The host screen announces everyone; a phone only announces
 * its own player, loudly.
 */
export default function SipToasts({ players, me }) {
  const [previous, setPrevious] = useState(players)
  const [toasts, setToasts] = useState([])

  // Compare with the last render's scores (state, not an effect, so nothing is missed or doubled)
  if (players !== previous) {
    setPrevious(players)
    const fresh = sipChanges(previous, players)
      .filter((change) => me.isHost || change.id === me.playerId)
      .map((change) => ({ ...change, key: nextId++, mine: change.id === me.playerId }))
    if (fresh.length > 0) setToasts((current) => [...current, ...fresh].slice(-5))
  }

  useEffect(() => {
    if (toasts.length === 0) return undefined
    const timer = setTimeout(() => setToasts([]), SHOW_MS)
    return () => clearTimeout(timer)
  }, [toasts])

  return (
    <div className="toasts" aria-live="assertive">
      {toasts.map((t) => (
        <div key={t.key} className={`toast ${t.mine ? 'toast--mine' : ''}`}>
          <span className="toast__sips">{t.sips}</span>
          <span className="toast__text">{t.mine ? `Drink ${t.sips}!` : `${t.name} drinks`}</span>
        </div>
      ))}
    </div>
  )
}
