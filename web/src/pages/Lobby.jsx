import { useState } from 'react'
import { lastCrew } from '../api/storage'
import { cleanName, MAX_NAME_LENGTH, MAX_PLAYERS, nameError } from '../lib/names'
import { useLeave } from '../lib/useLeave'
import GameIcon from '../components/GameIcon'
import JoinCard from '../components/JoinCard'
import { Confirm } from '../components/Modal'
import Shell from '../components/Shell'

function LineUp({ room }) {
  return (
    <ol className="lineup">
      {room.lineup.map((slot, i) => (
        <li key={slot.id} className={`lineup__item ${slot.playable ? '' : 'is-skipped'}`}>
          <span className="lineup__num">{i + 1}</span>
          <GameIcon gameId={slot.id} />
          <span className="lineup__name">{slot.name}</span>
          {!slot.playable && <span className="lineup__note">needs {slot.minPlayers}+</span>}
        </li>
      ))}
    </ol>
  )
}

function PlayerList({ room, me, onRemove }) {
  return (
    <ul className="roster">
      {room.players.map((p, i) => (
        <li key={p.id} className={`roster__row ${me.playerId === p.id ? 'is-me' : ''}`}>
          <span className="roster__num">{i + 1}</span>
          <span className="roster__name">{p.name}</span>
          <span className={`chip ${p.remote ? 'chip--phone' : ''}`}>{p.remote ? 'phone' : 'this screen'}</span>
          {onRemove && (
            <button type="button" className="btn btn--ghost btn--icon" onClick={() => onRemove(p)} aria-label={`Remove ${p.name}`}>✕</button>
          )}
        </li>
      ))}
      {room.players.length === 0 && <li className="roster__empty">Nobody yet. Add a name or scan the code.</li>}
    </ul>
  )
}

function HostLobby({ state }) {
  const { room, me, send, busy, error, token } = state
  const leave = useLeave({ room, me, token })
  const [name, setName] = useState('')
  const [touched, setTouched] = useState(false)
  const [closing, setClosing] = useState(false)

  const taken = room.players.map((p) => p.name)
  const problem = nameError(name, taken)
  const full = room.players.length >= MAX_PLAYERS
  const crew = lastCrew.get().filter((n) => !taken.some((t) => t.toLowerCase() === n.toLowerCase()))
  const quickGame = room.mode === 'QUICKPLAY' ? room.lineup[0] : null
  const tooFew = quickGame && !quickGame.playable
  const canStart = room.players.length > 0 && !tooFew

  const add = async (e) => {
    e.preventDefault()
    setTouched(true)
    if (problem || full) return
    if (await send('players', { name: cleanName(name) })) {
      setName('')
      setTouched(false)
    }
  }

  const addCrew = async () => {
    for (const n of crew.slice(0, MAX_PLAYERS - room.players.length)) {
      if (!(await send('players', { name: n }))) break
    }
  }

  const start = async () => {
    lastCrew.set(room.players.filter((p) => !p.remote).map((p) => p.name))
    await send('start')
  }

  return (
    <Shell
      middle={<span className="topbar__title">{quickGame ? `Quick Play · ${quickGame.name}` : 'Full Night'}</span>}
      actions={<button type="button" className="btn btn--ghost btn--sm" onClick={() => setClosing(true)}>Close</button>}
    >
      <div className="lobby">
        <section className="panel lobby__players">
          <h2 className="panel__title">Who's playing? <span className="panel__count">{room.players.length}/{MAX_PLAYERS}</span></h2>
          <PlayerList room={room} me={me} onRemove={(p) => send(`players/${p.id}`, undefined, { method: 'DELETE' })} />
          <form className="addrow" onSubmit={add} noValidate>
            <input className={`input ${touched && problem ? 'input--error' : ''}`} value={name} onChange={(e) => setName(e.target.value)}
              maxLength={MAX_NAME_LENGTH} placeholder={full ? 'Table is full' : 'Add a player on this screen'} disabled={full}
              aria-label="Player name" aria-invalid={Boolean(touched && problem)} autoComplete="off" />
            <button type="submit" className="btn" disabled={busy || full}>Add</button>
          </form>
          <p className="field-error" role="alert">{touched && problem && name ? problem : ''}</p>
          {crew.length > 0 && !full && (
            <button type="button" className="btn btn--ghost btn--sm lobby__crew" onClick={addCrew} disabled={busy}>
              + Last crew: {crew.join(', ')}
            </button>
          )}
        </section>

        <section className="panel lobby__join">
          <h2 className="panel__title">Play from your phones</h2>
          <JoinCard code={room.code} />
          <p className="lobby__hint">Everyone who scans gets their own seat and plays on their phone. This screen shows the table.</p>
        </section>

        <section className="panel lobby__start">
          {room.mode === 'FULL' ? <LineUp room={room} /> : (
            <div className="lobby__quick">
              <GameIcon gameId={quickGame.id} />
              <div>
                <strong>{quickGame.name}</strong>
                <span>{quickGame.tagline}</span>
              </div>
            </div>
          )}
          <div className="lobby__go">
            {error && <p className="alert" role="alert">{error}</p>}
            {tooFew && <p className="lobby__hint">{quickGame.name} needs at least {quickGame.minPlayers} players.</p>}
            <button type="button" className="btn btn--primary btn--lg" onClick={start} disabled={busy || !canStart}>
              {room.mode === 'FULL' ? 'Start the night' : `Start ${quickGame.name}`}
            </button>
          </div>
        </section>
      </div>

      {closing && (
        <Confirm title="Close this table?" confirmLabel="Close table" danger onCancel={() => setClosing(false)} onConfirm={leave}>
          Everyone who joined will be sent back to the start screen.
        </Confirm>
      )}
    </Shell>
  )
}

function PlayerLobby({ state }) {
  const { room, me, token } = state
  const leave = useLeave({ room, me, token })
  const mine = room.players.find((p) => p.id === me.playerId)

  return (
    <Shell middle={<span className="topbar__title">Table {room.code}</span>} fit={false}>
      <div className="join">
        <div className="panel join__card">
          <h1 className="page-title">You're in, {mine?.name}!</h1>
          <p className="join__hint waiting">Waiting for the host to start</p>
          <PlayerList room={room} me={me} />
          <button type="button" className="btn btn--ghost btn--sm" onClick={leave}>Leave the table</button>
        </div>
      </div>
    </Shell>
  )
}

export default function Lobby({ state }) {
  return state.me.isHost ? <HostLobby state={state} /> : <PlayerLobby state={state} />
}
