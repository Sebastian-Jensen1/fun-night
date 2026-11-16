import { useState } from 'react'
import { finalStandings } from '../lib/scores'
import { useLeave } from '../lib/useLeave'
import { Confirm } from '../components/Modal'
import Shell from '../components/Shell'
import { HandOut } from '../components/SipBank'

/** The end of the night: who drank what, a few awards, and a rematch with the same crew. */
export default function Final({ state }) {
  const { room, me, send, busy, error, token } = state
  const leave = useLeave({ room, me, token })
  const [leaving, setLeaving] = useState(false)
  const { rows, totalSips } = finalStandings(room.players)

  return (
    <Shell middle={<span className="topbar__title">Final scores</span>}>
      <div className="final">
        <header className="final__head">
          <h1 className="final__title">That's a night!</h1>
          <p className="final__total"><strong>{totalSips}</strong> sips went down</p>
        </header>

        <ol className="final__list panel">
          {rows.map((p) => (
            <li key={p.id} className={`final__row ${p.rank === 1 && p.thirstiest ? 'is-top' : ''} ${me.playerId === p.id ? 'is-me' : ''}`}>
              <span className="final__rank">{p.rank}</span>
              <span className="final__name">{p.name}</span>
              <span className="final__awards">
                {p.thirstiest && <span className="chip chip--award">Thirstiest</span>}
                {p.generous && <span className="chip chip--award chip--give">Most generous</span>}
                {p.dry && <span className="chip chip--award chip--dry">Bone dry</span>}
              </span>
              <span className="final__stat"><strong>{p.sipsDrunk}</strong> drunk</span>
              <span className="final__stat"><strong>{p.sipsGiven}</strong> given</span>
            </li>
          ))}
        </ol>

        <HandOut room={room} me={me} send={send} busy={busy} />
        {error && <p className="alert" role="alert">{error}</p>}

        <div className="final__actions">
          {me.isHost ? (
            <>
              <button type="button" className="btn btn--ghost" onClick={() => setLeaving(true)}>End the night</button>
              <button type="button" className="btn btn--primary btn--lg" onClick={() => send('rematch')} disabled={busy}>Rematch, same crew</button>
            </>
          ) : (
            <>
              <button type="button" className="btn btn--ghost" onClick={() => setLeaving(true)}>Leave</button>
              <p className="waiting">Waiting for the host: rematch?</p>
            </>
          )}
        </div>
      </div>

      {leaving && (
        <Confirm title={me.isHost ? 'End the night?' : 'Leave the table?'} confirmLabel={me.isHost ? 'End night' : 'Leave'} danger
          onCancel={() => setLeaving(false)} onConfirm={leave}>
          {me.isHost ? 'The table closes on every screen.' : "You won't be part of a rematch unless you scan the code again."}
        </Confirm>
      )}
    </Shell>
  )
}
