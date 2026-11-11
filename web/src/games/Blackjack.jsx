import PlayingCard from '../components/PlayingCard'
import Stepper from '../components/Stepper'

const OUTCOME = {
  BLACKJACK: { label: 'Blackjack!', tone: 'win' },
  WIN: { label: 'Wins', tone: 'win' },
  PUSH: { label: 'Push', tone: 'push' },
  LOSE: { label: 'Loses', tone: 'lose' },
  BUST: { label: 'Bust', tone: 'lose' },
}

function dealerLine(game, current) {
  if (game.phase === 'BETTING') return 'Place your bets'
  if (game.phase === 'PLAYER_TURNS') return `${current.name} to play`
  if (game.dealer.blackjack) return 'Dealer has blackjack'
  if (game.dealer.bust) return 'Dealer busts!'
  return `Dealer stands on ${game.dealer.value}`
}

function Seat({ seat, active, betting, canAct, onBet, busy, min, max }) {
  const outcome = seat.outcome ? OUTCOME[seat.outcome] : null
  return (
    <li className={`bj-seat ${active ? 'is-active' : ''} ${outcome ? `bj-seat--${outcome.tone}` : ''}`}>
      <div className="bj-seat__hand">
        <div className="hand">
          {seat.cards.map((card, i) => <PlayingCard key={i} card={card} />)}
          {seat.cards.length === 0 && <span className="hand__empty" />}
        </div>
        {seat.cards.length > 0 && (
          <span className={`pill ${seat.value > 21 ? 'pill--bad' : ''}`}>
            {seat.soft && seat.value < 21 ? `soft ${seat.value}` : seat.value}
          </span>
        )}
        {outcome && (
          <span className={`bj-seat__result bj-seat__result--${outcome.tone}`}>
            <strong>{outcome.label}</strong>
            {seat.sipsToDrink > 0 && <span>drinks {seat.sipsToDrink}</span>}
            {seat.sipsToGive > 0 && <span>gives {seat.sipsToGive}</span>}
          </span>
        )}
      </div>
      <div className="bj-seat__plate">
        <span className="bj-seat__name">{seat.name}</span>
        {betting && canAct ? (
          <Stepper value={seat.bet} min={min} max={max} onChange={onBet} disabled={busy} label={`${seat.name}'s bet`} />
        ) : (
          <span className="bj-seat__bet">
            {betting ? seat.bet : seat.stake} {(betting ? seat.bet : seat.stake) === 1 ? 'sip' : 'sips'}
            {!betting && seat.doubled && <em> ×2</em>}
          </span>
        )}
        {betting && seat.ready && <span className="bj-seat__ready" title="Bet locked in">✓</span>}
      </div>
    </li>
  )
}

/** Drinking blackjack against the dealer. All game logic runs on the server. */
export default function Blackjack({ game, me, send, busy }) {
  const playing = game.phase === 'PLAYER_TURNS'
  const current = game.seats.find((s) => s.playerId === game.currentPlayerId)
  const mine = game.seats.find((s) => s.playerId === me.playerId)
  const act = (action) => send('blackjack/action', { playerId: current.playerId, action })

  return (
    <div className="bj" data-seats={game.seats.length}>
      <div className="bj__dealer">
        <span className="bj__who">Dealer</span>
        <div className="hand">
          {game.dealer.cards.map((card, i) => <PlayingCard key={i} card={card} />)}
          {Array.from({ length: game.dealer.hiddenCards }, (_, i) => <PlayingCard key={`hole-${i}`} />)}
          {game.dealer.cards.length === 0 && <><PlayingCard /><PlayingCard /></>}
        </div>
        {game.dealer.value != null && (
          <span className={`pill ${game.dealer.bust ? 'pill--bad' : ''}`}>
            {game.dealer.hiddenCards > 0 ? `${game.dealer.value} + ?` : game.dealer.value}
          </span>
        )}
        <p className="bj__line" aria-live="polite">{dealerLine(game, current)}</p>
      </div>

      <ul className="bj__seats">
        {game.seats.map((seat) => (
          <Seat key={seat.playerId} seat={seat} active={playing && seat.playerId === game.currentPlayerId} betting={!playing}
            canAct={me.canActFor(seat.playerId)} busy={busy} min={game.minBet} max={game.maxBet}
            onBet={(sips) => send('blackjack/bet', { playerId: seat.playerId, sips })} />
        ))}
      </ul>

      <div className="actionbar">
        {playing && me.canActFor(current.playerId) && (
          <>
            <span className="actionbar__who">{current.playerId === me.playerId ? 'Your move' : current.name}</span>
            <button type="button" className="btn btn--primary" disabled={busy} onClick={() => act('HIT')}>Hit</button>
            <button type="button" className="btn" disabled={busy} onClick={() => act('STAND')}>Stand</button>
            <button type="button" className="btn" disabled={busy || !current.canDouble} onClick={() => act('DOUBLE')}>Double</button>
          </>
        )}
        {playing && !me.canActFor(current.playerId) && <p className="waiting">Waiting for {current.name}</p>}
        {!playing && me.isHost && (
          <button type="button" className="btn btn--primary btn--lg" disabled={busy} onClick={() => send('blackjack/deal')}>
            {game.round === 0 ? 'Deal' : 'Deal next round'}
          </button>
        )}
        {!playing && !me.isHost && mine && (
          mine.ready
            ? <p className="waiting">Bet locked in. Waiting for the deal</p>
            : (
              <button type="button" className="btn btn--primary btn--lg" disabled={busy}
                onClick={() => send('blackjack/bet', { playerId: mine.playerId, sips: mine.bet })}>
                Lock in {mine.bet} {mine.bet === 1 ? 'sip' : 'sips'}
              </button>
            )
        )}
      </div>
    </div>
  )
}
