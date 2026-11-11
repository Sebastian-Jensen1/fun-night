import PlayingCard from '../components/PlayingCard'

function verdict(result) {
  if (!result) return null
  if (result.passed) return { tone: 'push', text: `${result.playerName} passes the pile` }
  if (result.correct) return { tone: 'win', text: 'Correct!' }
  const same = result.from.rank === result.drawn.rank ? ' Same rank counts as wrong.' : ''
  return { tone: 'lose', text: `${result.playerName} drinks ${result.sipsDrunk}.${same}` }
}

/** Higher or Lower. The deck lives on the server so nobody can peek. */
export default function HigherLower({ game, me, send, busy }) {
  const { currentCard, currentPlayerId, currentPlayerName, streak, streakToPass, pile, sipsAtStake, canPass, lastResult } = game
  const canAct = me.canActFor(currentPlayerId)
  const isMe = currentPlayerId === me.playerId
  const guess = (g) => send('higher-lower/guess', { playerId: currentPlayerId, guess: g })
  const result = verdict(lastResult)
  const turnKey = `${pile}-${currentCard.rank}-${currentCard.suit}`

  return (
    <div className="hl">
      <p className="turnline" aria-live="polite">
        <span className="turnline__who">{isMe ? 'Your turn' : `${currentPlayerName}'s turn`}</span>
        <span className="turnline__what">Higher or lower than {currentCard.rank}?</span>
      </p>

      <div className="hl__table">
        <div className="hl__stat">
          <span className="hl__stat-label">Pile</span>
          <span className="hl__stat-value">{pile}</span>
          <span className="hl__stat-note">wrong = drink {sipsAtStake}</span>
        </div>

        <div className="hl__cards">
          {lastResult?.drawn && <PlayingCard card={lastResult.from} className="hl__previous" />}
          <PlayingCard card={currentCard} className="hl__current" key={turnKey} />
          <PlayingCard className="hl__deck" />
        </div>

        <div className="hl__stat">
          <span className="hl__stat-label">Streak</span>
          <span className="hl__dots" aria-label={`${streak} right in a row`}>
            {Array.from({ length: streakToPass }, (_, i) => <span key={i} className={`hl__dot ${i < streak ? 'is-lit' : ''}`} />)}
          </span>
          <span className="hl__stat-note">{canPass ? 'you may pass!' : `${streakToPass} in a row to pass`}</span>
        </div>
      </div>

      <p className={`verdict ${result ? `verdict--${result.tone}` : 'is-empty'}`} key={`v-${turnKey}-${currentPlayerId}`} aria-live="polite">
        {result ? result.text : 'Get it wrong and you drink the pile.'}
      </p>

      <div className="actionbar">
        {canAct ? (
          <>
            <button type="button" className="btn btn--primary btn--lg" disabled={busy} onClick={() => guess('HIGHER')}>▲ Higher</button>
            <button type="button" className="btn btn--danger btn--lg" disabled={busy} onClick={() => guess('LOWER')}>▼ Lower</button>
            {canPass && (
              <button type="button" className="btn btn--lg" disabled={busy}
                onClick={() => send('higher-lower/pass', { playerId: currentPlayerId })}>
                Pass the pile →
              </button>
            )}
          </>
        ) : <p className="waiting">Waiting for {currentPlayerName}</p>}
      </div>
    </div>
  )
}
