import { isRed, SUIT_NAME } from '../lib/suits'
import Suit from './Suit'

const RANK_NAME = { A: 'Ace', K: 'King', Q: 'Queen', J: 'Jack' }

/** One playing card, sized by the --cw custom property of its container. Without a card it shows the back. */
export default function PlayingCard({ card, className = '', style }) {
  if (!card) {
    return <div className={`pcard pcard--back ${className}`} style={style} role="img" aria-label="Face-down card" />
  }
  const label = `${RANK_NAME[card.rank] ?? card.rank} of ${SUIT_NAME[card.suit]}`
  return (
    <div className={`pcard pcard--${isRed(card.suit) ? 'red' : 'black'} ${className}`} style={style} role="img" aria-label={label}>
      <span className="pcard__corner" aria-hidden="true">
        <span className="pcard__rank">{card.rank}</span>
        <Suit suit={card.suit} />
      </span>
      <Suit suit={card.suit} className="pcard__pip" />
    </div>
  )
}
