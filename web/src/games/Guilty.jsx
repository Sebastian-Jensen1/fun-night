import { useState } from 'react'
import Modal from '../components/Modal'

function Prompt({ game }) {
  return (
    <div className="guilty__prompt">
      <span className="guilty__lead">Who is most likely to…</span>
      <p className="guilty__question" key={game.round}>{game.prompt}</p>
    </div>
  )
}

function Verdict({ game, send, busy }) {
  const { verdict } = game
  const rows = [...verdict.tally].sort((a, b) => b.votes - a.votes)
  const most = Math.max(1, rows[0].votes)
  const guilty = rows.filter((r) => r.guilty).map((r) => r.playerName)
  return (
    <>
      <ul className="tally">
        {rows.map((row) => (
          <li key={row.playerId} className={`tally__row ${row.guilty ? 'is-guilty' : ''}`}>
            <span className="tally__name">{row.playerName}</span>
            <span className="tally__bar"><span style={{ width: `${(row.votes / most) * 100}%` }} /></span>
            <span className="tally__votes">{row.votes}</span>
          </li>
        ))}
      </ul>
      <p className="verdict verdict--lose" aria-live="polite">
        Guilty: {guilty.join(' & ')}. Drink {verdict.sips}!
      </p>
      <div className="actionbar">
        <button type="button" className="btn btn--primary btn--lg" disabled={busy} onClick={() => send('guilty/next')}>Next question</button>
      </div>
    </>
  )
}

/** On a phone: vote for whoever fits the question. The vote can be changed until everyone is in. */
function PhoneVote({ room, game, me, send, busy }) {
  return (
    <>
      <div className="picker picker--big">
        {room.players.map((p) => (
          <button key={p.id} type="button" className={`btn ${game.yourVote === p.id ? 'btn--primary is-on' : ''}`} disabled={busy}
            onClick={() => send('guilty/vote', { playerId: me.playerId, accusedId: p.id })}>
            {p.id === me.playerId ? `${p.name} (me)` : p.name}
          </button>
        ))}
      </div>
      <p className="waiting">
        {game.yourVote ? 'Vote locked in' : 'Tap a name to vote'} · {game.voted.length}/{room.players.length} voted
      </p>
    </>
  )
}

/**
 * On the shared screen: phones vote for themselves; everyone else passes the screen round,
 * taps their own name and votes in a dialog that closes as soon as they pick.
 */
function HostVote({ room, game, send, busy }) {
  const [voterId, setVoterId] = useState(null)
  const voter = room.players.find((p) => p.id === voterId)
  const cast = async (accusedId) => {
    setVoterId(null)
    await send('guilty/vote', { playerId: voterId, accusedId })
  }

  return (
    <>
      <ul className="voters">
        {room.players.map((p) => {
          const voted = game.voted.includes(p.id)
          return (
            <li key={p.id} className={`voter ${voted ? 'is-done' : ''}`}>
              <span className="voter__name">{p.name}</span>
              <span className="voter__state">{voted ? 'voted' : p.remote ? 'on their phone…' : 'your turn to vote'}</span>
              <button type="button" className={`btn btn--sm ${voted || p.remote ? 'btn--ghost' : 'btn--primary'}`} disabled={busy}
                onClick={() => setVoterId(p.id)}>
                {voted ? 'Change' : p.remote ? 'Vote here' : 'Vote'}
              </button>
            </li>
          )
        })}
      </ul>
      <div className="actionbar">
        <button type="button" className="btn" disabled={busy || game.voted.length === 0} onClick={() => send('guilty/close')}>
          Read the verdict now ({game.voted.length}/{room.players.length} voted)
        </button>
      </div>

      {voter && (
        <Modal title={`${voter.name}, who's guilty?`} onClose={() => setVoterId(null)}>
          <p className="modal__note">Everyone else: look away! “{game.prompt}”</p>
          <div className="picker picker--big">
            {room.players.map((p) => (
              <button key={p.id} type="button" className="btn" onClick={() => cast(p.id)}>
                {p.id === voter.id ? `${p.name} (me)` : p.name}
              </button>
            ))}
          </div>
        </Modal>
      )}
    </>
  )
}

/** Guilty!: a secret vote on who a question fits best. */
export default function Guilty({ room, game, me, send, busy }) {
  return (
    <div className="guilty">
      <Prompt game={game} />
      {game.phase === 'VERDICT' && <Verdict game={game} send={send} busy={busy} />}
      {game.phase === 'VOTING' && (me.isHost
        ? <HostVote room={room} game={game} send={send} busy={busy} />
        : <PhoneVote room={room} game={game} me={me} send={send} busy={busy} />)}
    </div>
  )
}
