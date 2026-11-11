import { useEffect, useState } from 'react'
import PlayingCard from '../components/PlayingCard'
import Stepper from '../components/Stepper'
import Suit from '../components/Suit'
import { SUIT_NAME, SUITS } from '../lib/suits'

const FLIP_EVERY_MS = 1200
const cap = (word) => word[0].toUpperCase() + word.slice(1)

function Track({ game }) {
  const steps = game.trackLength
  return (
    <div className="track" style={{ '--steps': steps }}>
      <div className="track__hurdles" aria-label="Hurdles">
        {game.hurdles.map((card, i) => (
          <div key={i} className="track__hurdle" style={{ '--pos': i + 1 }}>
            <PlayingCard card={card} />
          </div>
        ))}
      </div>
      {SUITS.map((suit) => {
        const pos = game.positions[suit]
        return (
          <div key={suit} className={`lane ${game.winner === suit ? 'is-winner' : ''} ${game.lastStumble === suit ? 'is-stumble' : ''}`}
            aria-label={`${cap(SUIT_NAME[suit])}: step ${pos} of ${steps}`}>
            <div className="lane__marks" aria-hidden="true">
              {Array.from({ length: steps + 1 }, (_, i) => <span key={i} className="lane__mark" />)}
            </div>
            <div className="lane__horse" style={{ '--pos': pos }}>
              <PlayingCard card={{ rank: 'A', suit }} />
            </div>
          </div>
        )
      })}
      <div className="track__finish" aria-hidden="true" />
    </div>
  )
}

function BetRow({ player, bet, game, send, busy, isMe }) {
  const [draftSips, setDraftSips] = useState(1)
  const sips = bet?.sips ?? draftSips
  const place = (suit, amount) => send('derby/bet', { playerId: player.id, suit, sips: amount })
  const changeSips = (amount) => {
    setDraftSips(amount)
    if (bet) place(bet.suit, amount)
  }
  return (
    <li className={`betrow ${bet ? 'is-set' : ''}`}>
      <span className="betrow__name">{isMe ? 'You' : player.name}</span>
      <div className="betrow__suits" role="radiogroup" aria-label={`${player.name}'s suit`}>
        {SUITS.map((suit) => (
          <button key={suit} type="button" role="radio" aria-checked={bet?.suit === suit} aria-label={SUIT_NAME[suit]}
            className={`suitbtn ${bet?.suit === suit ? 'is-on' : ''}`} disabled={busy} onClick={() => place(suit, sips)}>
            <Suit suit={suit} />
          </button>
        ))}
      </div>
      <Stepper value={sips} min={game.minBet} max={game.maxBet} onChange={changeSips} disabled={busy} label={`${player.name}'s sips`} />
    </li>
  )
}

function BetsBySuit({ game }) {
  return (
    <ul className="backers">
      {SUITS.map((suit) => {
        const backers = game.bets.filter((b) => b.suit === suit)
        return (
          <li key={suit} className={`backers__row ${game.winner === suit ? 'is-winner' : ''}`}>
            <Suit suit={suit} />
            <span className="backers__names">
              {backers.length === 0 ? '—' : backers.map((b) => `${b.playerName} ${b.sips}`).join(', ')}
            </span>
          </li>
        )
      })}
    </ul>
  )
}

/** Derby: back a suit, then watch the four aces race as cards are turned. */
export default function Derby({ room, game, me, send, busy }) {
  const [auto, setAuto] = useState(true)
  const racing = game.phase === 'RACING'

  // The host screen turns the cards on a timer so the race runs by itself
  useEffect(() => {
    if (!(me.isHost && auto && racing)) return undefined
    const timer = setTimeout(() => send('derby/flip', undefined, { quiet: true }), FLIP_EVERY_MS)
    return () => clearTimeout(timer)
  }, [me.isHost, auto, racing, game.flips, send])

  const betOf = (playerId) => game.bets.find((b) => b.playerId === playerId)
  const mine = room.players.filter((p) => me.canActFor(p.id))

  return (
    <div className="derby">
      <Track game={game} />

      <div className="derby__side">
        {game.phase === 'BETTING' && (
          <>
            <p className="turnline">
              <span className="turnline__who">Race {game.race}</span>
              <span className="turnline__what">Back a suit. {game.bets.length}/{room.players.length} bets in.</span>
            </p>
            <ul className="betrows">
              {mine.map((p) => (
                <BetRow key={p.id} player={p} bet={betOf(p.id)} game={game} send={send} busy={busy} isMe={p.id === me.playerId} />
              ))}
            </ul>
            {!me.isHost && <BetsBySuit game={game} />}
            <div className="actionbar">
              {me.isHost
                ? <button type="button" className="btn btn--primary btn--lg" disabled={busy || game.bets.length === 0} onClick={() => send('derby/start')}>They're off!</button>
                : <p className="waiting">{betOf(me.playerId) ? 'Bet placed. Waiting for the start' : 'Pick a suit to bet'}</p>}
            </div>
          </>
        )}

        {racing && (
          <>
            <p className="turnline" aria-live="polite">
              <span className="turnline__who">
                {game.lastStumble ? `${cap(SUIT_NAME[game.lastStumble])} stumbles!` : game.lastFlip ? `${cap(SUIT_NAME[game.lastFlip.suit])} moves up` : "They're off!"}
              </span>
              <span className="turnline__what">Card {game.flips}</span>
            </p>
            <div className="derby__flip">
              <PlayingCard card={game.lastFlip} key={game.flips} />
              <BetsBySuit game={game} />
            </div>
            <div className="actionbar">
              <button type="button" className="btn btn--primary" disabled={busy} onClick={() => send('derby/flip')}>Turn a card</button>
              {me.isHost && (
                <button type="button" className={`btn btn--sm ${auto ? 'is-on' : 'btn--ghost'}`} aria-pressed={auto} onClick={() => setAuto(!auto)}>
                  Auto {auto ? 'on' : 'off'}
                </button>
              )}
            </div>
          </>
        )}

        {game.phase === 'FINISHED' && (
          <>
            <p className="turnline" aria-live="polite">
              <span className="turnline__who">{cap(SUIT_NAME[game.winner])} wins!</span>
              <span className="turnline__what">in {game.flips} cards</span>
            </p>
            <ul className="payouts">
              {game.payouts.map((p) => (
                <li key={p.playerId} className={`payouts__row ${p.sipsToGive > 0 ? 'is-win' : 'is-lose'}`}>
                  <Suit suit={p.suit} />
                  <span className="payouts__name">{p.playerName}</span>
                  <span className="payouts__what">{p.sipsToGive > 0 ? `gives ${p.sipsToGive}` : `drinks ${p.sipsToDrink}`}</span>
                </li>
              ))}
            </ul>
            <div className="actionbar">
              {me.isHost
                ? <button type="button" className="btn btn--primary btn--lg" disabled={busy} onClick={() => send('derby/again')}>Line up a new race</button>
                : <p className="waiting">Waiting for the host</p>}
            </div>
          </>
        )}
      </div>
    </div>
  )
}
