import GameIcon from './GameIcon'
import './SeedPacket.css'

/** A game shown as a garden seed packet. Locked packets are greyed out and not clickable. */
export default function SeedPacket({ game, onClick, compact = false }) {
  const locked = !game.available
  return (
    <button
      type="button"
      className={`seed-packet ${locked ? 'seed-packet--locked' : ''} ${compact ? 'seed-packet--compact' : ''}`}
      onClick={onClick}
      disabled={locked}
      aria-label={locked ? `${game.name}: coming soon` : `Play ${game.name}`}
    >
      <span className="seed-packet__number display">{game.order}</span>
      <span className="seed-packet__art">
        <GameIcon gameId={locked ? 'mystery' : game.id} size={compact ? 48 : 72} />
      </span>
      <span className="seed-packet__name display">{game.name}</span>
      {!compact && <span className="seed-packet__tagline">{locked ? 'Coming soon!' : game.tagline}</span>}
    </button>
  )
}
