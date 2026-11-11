import GameIcon from './GameIcon'

/** The night's line-up as a row of stops: done, now playing, coming up, skipped. */
export default function Trail({ lineup }) {
  return (
    <ol className="trail" aria-label="Tonight's games">
      {lineup.map((slot, i) => (
        <li key={slot.id} className={`trail__stop trail__stop--${slot.status.toLowerCase()}`}
          title={slot.status === 'SKIPPED' ? `${slot.name} (needs ${slot.minPlayers}+ players)` : slot.name}>
          <span className="trail__num">{i + 1}</span>
          <GameIcon gameId={slot.id} />
          <span className="sr-only">
            {slot.name}
            {slot.status === 'ACTIVE' && ' (now playing)'}
            {slot.status === 'COMPLETED' && ' (done)'}
            {slot.status === 'SKIPPED' && ' (skipped)'}
          </span>
        </li>
      ))}
    </ol>
  )
}
