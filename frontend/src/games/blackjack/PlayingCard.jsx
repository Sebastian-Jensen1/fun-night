const SUITS = {
  HEARTS: { symbol: '♥', red: true, name: 'hearts' },
  DIAMONDS: { symbol: '♦', red: true, name: 'diamonds' },
  CLUBS: { symbol: '♣', red: false, name: 'clubs' },
  SPADES: { symbol: '♠', red: false, name: 'spades' },
}

/** A single playing card. Pass no card to show the back (the dealer's hole card). */
export default function PlayingCard({ card, index = 0 }) {
  const style = { '--i': index }
  if (!card) {
    return (
      <div className="card card--back" style={style} role="img" aria-label="Face-down card">
        <span className="card__back-art" aria-hidden="true">🧠</span>
      </div>
    )
  }
  const suit = SUITS[card.suit]
  return (
    <div
      className={`card ${suit.red ? 'card--red' : ''}`}
      style={style}
      role="img"
      aria-label={`${card.rank} of ${suit.name}`}
    >
      <span className="card__corner" aria-hidden="true">
        {card.rank}
        <br />
        {suit.symbol}
      </span>
      <span className="card__pip" aria-hidden="true">{suit.symbol}</span>
      <span className="card__corner card__corner--flip" aria-hidden="true">
        {card.rank}
        <br />
        {suit.symbol}
      </span>
    </div>
  )
}
