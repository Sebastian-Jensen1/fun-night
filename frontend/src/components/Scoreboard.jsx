import './Scoreboard.css'

/**
 * The sip bank: running totals of sips each player has drunk and handed out, styled like
 * the resource counter in a garden-defence game.
 */
export default function Scoreboard({ players, highlightId }) {
  return (
    <section className="scoreboard" aria-label="Sip scoreboard">
      <h2 className="scoreboard__title display">Sip Bank</h2>
      <ul className="scoreboard__list">
        {players.map((p) => (
          <li key={p.id} className={`scoreboard__player ${p.id === highlightId ? 'scoreboard__player--active' : ''}`}>
            <span className="scoreboard__name">{p.name}</span>
            <span className="scoreboard__stat scoreboard__stat--drunk" title="Sips drunk">
              <span aria-hidden="true">🍺</span> {p.sipsDrunk}
              <span className="sr-only"> sips drunk</span>
            </span>
            <span className="scoreboard__stat scoreboard__stat--given" title="Sips handed out">
              <span aria-hidden="true">👉</span> {p.sipsGiven}
              <span className="sr-only"> sips handed out</span>
            </span>
          </li>
        ))}
      </ul>
    </section>
  )
}
