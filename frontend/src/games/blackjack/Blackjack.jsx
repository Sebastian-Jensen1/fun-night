import { useEffect, useState } from 'react'
import { api } from '../../api/client'
import Dealer from '../../components/Dealer'
import PlayingCard from '../../components/PlayingCard'
import MusicToggle from './MusicToggle'
import './Blackjack.css'

const DEFAULT_BET = 2

const OUTCOME_LABEL = {
  BLACKJACK: 'Blackjack!',
  WIN: 'Win!',
  PUSH: 'Push',
  LOSE: 'Lose',
  BUST: 'Bust!',
}

function dealerMood(table) {
  if (!table || table.phase !== 'ROUND_OVER') return 'idle'
  if (table.dealer.bust) return 'sad'
  const outcomes = table.seats.map((s) => s.outcome)
  if (outcomes.includes('BLACKJACK')) return 'shock'
  const playerWins = outcomes.filter((o) => o === 'WIN').length
  const houseWins = outcomes.filter((o) => o === 'LOSE' || o === 'BUST').length
  return houseWins >= playerWins ? 'happy' : 'sad'
}

function dealerLine(table, mood) {
  if (!table || table.round === 0) {
    return "Welcome to my table! Bet 1–5 sips each. Lose and you drink 'em, win and you hand 'em out."
  }
  if (table.phase === 'PLAYER_TURNS') {
    const seat = table.seats.find((s) => s.playerId === table.currentPlayerId)
    return `${seat.playerName}, you've got ${seat.soft ? 'soft ' : ''}${seat.value}. Hit or stand?`
  }
  if (table.dealer.blackjack) return 'Blackjack! The house always wins. Bottoms up!'
  if (table.dealer.bust) return `${table.dealer.value}?! I busted! Go on, hand 'em out…`
  if (mood === 'shock') return 'A natural blackjack?! Who taught you that?'
  if (mood === 'happy') return 'House wins! Drink up, buttercups.'
  return 'Lucky lot. Hand out those sips before I change my mind.'
}

function SipChips({ count }) {
  return (
    <span className="sip-chips" aria-label={`${count} sips`}>
      {Array.from({ length: Math.min(count, 10) }, (_, i) => (
        <span key={i} className="sip-chip" aria-hidden="true" />
      ))}
    </span>
  )
}

function Seat({ player, seat, active, canBet, bet, onBet, min, max }) {
  const outcome = seat?.outcome
  return (
    <li className={`bj-seat ${active ? 'bj-seat--active' : ''} ${outcome ? `bj-seat--${outcome.toLowerCase()}` : ''}`}>
      {active && <span className="bj-seat__arrow" aria-hidden="true">▼</span>}
      <div className="bj-seat__cards">
        {seat ? seat.cards.map((c, i) => <PlayingCard key={i} card={c} index={i} />) : <div className="bj-seat__empty" />}
      </div>
      {seat && (
        <span className={`bj-value ${seat.value > 21 ? 'bj-value--bust' : ''}`}>
          {seat.soft && seat.value <= 21 ? `soft ${seat.value}` : seat.value}
        </span>
      )}
      {outcome && (
        <div className="bj-result pop-in">
          <strong className="display">{OUTCOME_LABEL[outcome]}</strong>
          {seat.sipsToDrink > 0 && <span>Drink {seat.sipsToDrink}</span>}
          {seat.sipsToGive > 0 && <span>Give out {seat.sipsToGive}</span>}
        </div>
      )}
      <div className="bj-seat__plate">
        <span className="bj-seat__name">{player.name}</span>
        {canBet ? (
          <div className="bj-bet" role="group" aria-label={`${player.name}'s bet`}>
            <button type="button" className="bj-bet__btn" onClick={() => onBet(bet - 1)} disabled={bet <= min} aria-label="Bet one sip less">−</button>
            <span className="bj-bet__amount" aria-live="polite">{bet} 🍺</span>
            <button type="button" className="bj-bet__btn" onClick={() => onBet(bet + 1)} disabled={bet >= max} aria-label="Bet one sip more">+</button>
          </div>
        ) : (
          seat && (
            <span className="bj-seat__bet">
              <SipChips count={seat.bet} /> {seat.bet} {seat.doubled && <em className="bj-seat__double">×2</em>}
            </span>
          )
        )}
      </div>
    </li>
  )
}

/** Drinking blackjack against Dealer Dan. All game logic runs server-side. */
export default function Blackjack({ sessionId, token, players, onScoresChanged }) {
  const [table, setTable] = useState(null)
  const [bets, setBets] = useState({})
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState(null)

  useEffect(() => {
    let cancelled = false
    api.blackjack
      .get(sessionId, token)
      .then((t) => !cancelled && setTable(t))
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
      setTable(next)
      if (next.phase === 'ROUND_OVER') onScoresChanged()
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  const betFor = (id) => bets[id] ?? DEFAULT_BET

  const deal = () => {
    const placed = Object.fromEntries(players.map((p) => [p.id, betFor(p.id)]))
    setBets(placed)
    run(() => api.blackjack.deal(sessionId, token, placed))
  }
  const act = (action) => run(() => api.blackjack.act(sessionId, token, table.currentPlayerId, action))

  if (!table) {
    return error ? <p className="error-banner" role="alert">{error}</p> : <p className="panel">Shuffling…</p>
  }

  const mood = dealerMood(table)
  const playing = table.phase === 'PLAYER_TURNS'
  const current = table.seats.find((s) => s.playerId === table.currentPlayerId)

  return (
    <div className="bj">
      <MusicToggle />
      <div className="bj__dealer-area">
        <div className="bj__dealer">
          <Dealer mood={mood} />
        </div>
        <p className="bj__speech" aria-live="polite">{dealerLine(table, mood)}</p>
      </div>

      <div className="bj__table">
        <div className="bj__dealer-hand" aria-label="Dealer's hand">
          <div className="bj__dealer-cards">
            {table.dealer.cards.map((c, i) => (
              <PlayingCard key={`up-${i}`} card={c} index={i} />
            ))}
            {Array.from({ length: table.dealer.hiddenCards }, (_, i) => (
              <PlayingCard key={`hole-${i}`} index={table.dealer.cards.length + i} />
            ))}
          </div>
          {table.dealer.value != null && (
            <span className={`bj-value ${table.dealer.bust ? 'bj-value--bust' : ''}`}>
              {table.dealer.hiddenCards > 0 ? `${table.dealer.value} + ?` : table.dealer.value}
            </span>
          )}
        </div>

        <p className="bj__rules-strip display">Blackjack pays 3:2 · Dealer stands on 17</p>

        <ul className="bj__seats">
          {players.map((p) => (
            <Seat
              key={p.id}
              player={p}
              seat={table.seats.find((s) => s.playerId === p.id)}
              active={playing && p.id === table.currentPlayerId}
              canBet={!playing}
              bet={betFor(p.id)}
              onBet={(n) => setBets((b) => ({ ...b, [p.id]: n }))}
              min={table.minBet}
              max={table.maxBet}
            />
          ))}
        </ul>
      </div>

      {error && <p className="error-banner" role="alert">{error}</p>}

      <div className="bj__controls">
        {playing ? (
          <>
            <button type="button" className="btn btn--green" onClick={() => act('HIT')} disabled={busy}>Hit</button>
            <button type="button" className="btn btn--red" onClick={() => act('STAND')} disabled={busy}>Stand</button>
            <button type="button" className="btn" onClick={() => act('DOUBLE')} disabled={busy || !current?.canDouble}>
              Double
            </button>
          </>
        ) : (
          <button type="button" className="btn btn--big" onClick={deal} disabled={busy}>
            {table.round === 0 ? 'Deal! 🃏' : 'Next round 🃏'}
          </button>
        )}
      </div>

      <details className="panel bj__howto">
        <summary className="display">How to play</summary>
        <ul>
          <li>Everyone bets 1–5 sips, then plays their hand against the dealer.</li>
          <li><b>Hit</b> to take a card, <b>Stand</b> to stop, <b>Double</b> to double your bet for exactly one more card.</li>
          <li>Beat the dealer without going over 21: hand out your bet. Natural blackjack: hand out 1.5×.</li>
          <li>Lose or bust: drink your bet. Tie: nobody drinks.</li>
        </ul>
      </details>
    </div>
  )
}
