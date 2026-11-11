import PlayingCard from '../components/PlayingCard'

function KingsCup({ game }) {
  const capacity = game.cupSipsPerKing * (game.kingsTotal - 1)
  const fill = Math.min(1, game.cupSips / capacity)
  return (
    <div className="cup" aria-label={`King's Cup: ${game.kingsDrawn} of ${game.kingsTotal} kings pulled, ${game.cupSips} sips in the cup`}>
      <div className="cup__glass" aria-hidden="true">
        <div className="cup__drink" style={{ height: `${fill * 100}%` }} />
      </div>
      <div className="cup__kings" aria-hidden="true">
        {Array.from({ length: game.kingsTotal }, (_, i) => <span key={i} className={`cup__king ${i < game.kingsDrawn ? 'is-on' : ''}`}>K</span>)}
      </div>
      <span className="cup__sips">{game.cupSips} sips</span>
    </div>
  )
}

function describe(result) {
  const drinks = result.drinks.map((d) => `${d.playerName} ${d.sips}`).join(', ')
  return `${result.playerName} pulled ${result.card.rank}: ${result.ruleTitle}.${drinks ? ` Drinks: ${drinks}.` : ''}`
}

function Resolve({ game, players, me, send, busy }) {
  const { rule, currentCard, currentPlayerId, currentPlayerName } = game
  const canAct = me.canActFor(currentPlayerId)
  const resolve = (targetId) => send('ring-of-fire/resolve', { playerId: currentPlayerId, targetId })
  const pickable = players.filter((p) => rule.target === 'ANYONE' || p.id !== currentPlayerId || players.length === 1)
  const prompt = rule.title === 'Mate' ? 'Pick your mate' : rule.target === 'OTHER' ? 'Who drinks?' : 'Who lost?'

  return (
    <div className="rof-rule" key={`${currentCard.rank}-${currentCard.suit}-${game.cardsLeft}`}>
      <PlayingCard card={currentCard} />
      <div className="rof-rule__body">
        <span className="rof-rule__who">{currentPlayerName} pulled</span>
        <h3 className="rof-rule__title">{rule.title}</h3>
        <p className="rof-rule__desc">{rule.description}</p>
        {!canAct && <p className="waiting">Waiting for {currentPlayerName}</p>}
        {canAct && rule.target === 'NONE' && (
          <button type="button" className="btn btn--primary" disabled={busy} onClick={() => resolve(null)}>Done</button>
        )}
        {canAct && rule.target !== 'NONE' && (
          <>
            <span className="rof-rule__prompt">{prompt}</span>
            <div className="picker">
              {pickable.map((p) => (
                <button key={p.id} type="button" className="btn btn--sm" disabled={busy} onClick={() => resolve(p.id)}>{p.name}</button>
              ))}
            </div>
          </>
        )}
      </div>
    </div>
  )
}

/** The rule for every rank, shown in the "how to play" dialog. */
export function RingRules({ game }) {
  return (
    <dl className="rof-rules">
      {game.rules.map((rule) => (
        <div key={rule.rank} className="rof-rules__row">
          <dt><span className="rof-rules__rank">{rule.rank}</span>{rule.title}</dt>
          <dd>{rule.description}</dd>
        </div>
      ))}
    </dl>
  )
}

/** Ring of Fire (King's Cup). The deck is shuffled on the server; the ring is for show. */
export default function RingOfFire({ room, game, me, send, busy }) {
  const { phase, currentPlayerId, currentPlayerName, cardsLeft, ringSize } = game
  const canAct = me.canActFor(currentPlayerId)
  const isMe = currentPlayerId === me.playerId

  return (
    <div className="rof">
      <div className="rof__ringwrap">
        <div className="rof__ring" aria-hidden="true">
          {Array.from({ length: cardsLeft }, (_, i) => (
            <span key={i} className="rof__slot" style={{ '--a': `${(i * 360) / ringSize}deg` }} />
          ))}
        </div>
        <KingsCup game={game} />
      </div>

      <div className="rof__panel">
        <p className="turnline" aria-live="polite">
          <span className="turnline__who">
            {phase === 'OVER' ? 'The ring is broken!' : isMe ? 'Your turn' : `${currentPlayerName}'s turn`}
          </span>
          <span className="turnline__what">{cardsLeft} cards left in the ring</span>
        </p>

        {phase === 'RESOLVE' && <Resolve game={game} players={room.players} me={me} send={send} busy={busy} />}

        {phase === 'DRAW' && (
          <div className="rof__draw">
            {canAct ? (
              <button type="button" className="btn btn--primary btn--lg" disabled={busy}
                onClick={() => send('ring-of-fire/draw', { playerId: currentPlayerId })}>
                Pull a card
              </button>
            ) : <p className="waiting">Waiting for {currentPlayerName}</p>}
            {game.lastResult && <p className="lastline" key={cardsLeft}>{describe(game.lastResult)}</p>}
          </div>
        )}

        {phase === 'OVER' && (
          <div className="rof__draw">
            <p className="lastline">{describe(game.lastResult)}</p>
            {me.isHost
              ? <button type="button" className="btn btn--primary btn--lg" disabled={busy} onClick={() => send('ring-of-fire/restart')}>Lay out a new ring</button>
              : <p className="waiting">Waiting for the host</p>}
          </div>
        )}

        {(game.questionMaster || game.mates.length > 0) && (
          <div className="rof__status">
            {game.questionMaster && <span className="chip">Question Master: {game.questionMaster}</span>}
            {game.mates.map(([a, b]) => <span key={`${a}-${b}`} className="chip">Mates: {a} + {b}</span>)}
          </div>
        )}
      </div>
    </div>
  )
}
