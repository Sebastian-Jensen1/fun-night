const LEVEL_NAME = { MILD: 'Mild', WILD: 'Wild' }

/** Dare or Drink: pick mild or wild, then do the dare or pay in sips. */
export default function Dares({ game, me, send, busy }) {
  const { phase, currentPlayerId, currentPlayerName, lastResult } = game
  const canAct = me.canActFor(currentPlayerId)
  const isMe = currentPlayerId === me.playerId
  const who = isMe ? 'You' : currentPlayerName
  const stakes = game.levels.find((l) => l.id === game.level)

  return (
    <div className="dares">
      <p className="turnline" aria-live="polite">
        <span className="turnline__who">{isMe ? 'Your turn' : `${currentPlayerName}'s turn`}</span>
        <span className="turnline__what">{phase === 'CHOOSE' ? 'How brave are you feeling?' : 'Do it, or drink.'}</span>
      </p>

      {phase === 'CHOOSE' ? (
        <div className="dares__choices">
          {game.levels.map((level) => (
            <button key={level.id} type="button" className={`dare-pick dare-pick--${level.id.toLowerCase()}`}
              disabled={busy || !canAct} onClick={() => send('dares/draw', { playerId: currentPlayerId, level: level.id })}>
              <span className="dare-pick__name">{LEVEL_NAME[level.id]}</span>
              <span className="dare-pick__stakes">
                <span>do it <strong>+{level.sipsToGive}</strong> to give</span>
                <span>chicken out <strong>{level.sipsToDrink}</strong> to drink</span>
              </span>
            </button>
          ))}
        </div>
      ) : (
        <div className={`dare-card dare-card--${game.level.toLowerCase()}`} key={game.turn}>
          <span className="dare-card__level">{LEVEL_NAME[game.level]} dare</span>
          <p className="dare-card__text">{game.dare}</p>
        </div>
      )}

      <div className="actionbar">
        {phase === 'RESOLVE' && canAct && (
          <>
            <button type="button" className="btn btn--primary btn--lg" disabled={busy}
              onClick={() => send('dares/resolve', { playerId: currentPlayerId, completed: true })}>
              Did it · give {stakes.sipsToGive}
            </button>
            <button type="button" className="btn btn--danger btn--lg" disabled={busy}
              onClick={() => send('dares/resolve', { playerId: currentPlayerId, completed: false })}>
              Chicken out · drink {stakes.sipsToDrink}
            </button>
          </>
        )}
        {!canAct && <p className="waiting">Waiting for {currentPlayerName}</p>}
        {phase === 'CHOOSE' && canAct && lastResult && (
          <p className="lastline">
            {lastResult.completed
              ? `${lastResult.playerName} did it and has ${lastResult.sipsToGive} to hand out.`
              : `${lastResult.playerName} chickened out and drinks ${lastResult.sipsDrunk}.`}
          </p>
        )}
        {phase === 'CHOOSE' && canAct && !lastResult && <p className="lastline">{who} pick{isMe ? '' : 's'} a deck above.</p>}
      </div>
    </div>
  )
}
