import { useEffect, useState } from 'react'
import { GAMES } from '../games'
import { useLeave } from '../lib/useLeave'
import { useWakeLock } from '../lib/useWakeLock'
import GameIcon from '../components/GameIcon'
import JoinCard from '../components/JoinCard'
import Modal, { Confirm } from '../components/Modal'
import Shell from '../components/Shell'
import SipBank, { HandOut } from '../components/SipBank'
import SipToasts from '../components/SipToasts'
import Trail from '../components/Trail'

const ERROR_SHOW_MS = 4500

function ProgressPill({ progress }) {
  const done = Math.min(progress.done, progress.goal)
  return (
    <span className={`progress ${progress.done >= progress.goal ? 'is-done' : ''}`}
      title="A suggestion for when to move on. The host decides.">
      <span className="progress__bar" aria-hidden="true">
        <span style={{ width: `${(done / progress.goal) * 100}%` }} />
      </span>
      <span className="progress__text">{done}/{progress.goal} {progress.unit}</span>
    </span>
  )
}

export default function Play({ state }) {
  const { room, me, send, busy, error, clearError, offline, token } = state
  const leave = useLeave({ room, me, token })
  const [dialog, setDialog] = useState(null) // 'next' | 'leave' | 'rules' | 'join' | { release: player }
  useWakeLock(true)

  useEffect(() => {
    if (!error) return undefined
    const timer = setTimeout(clearError, ERROR_SHOW_MS)
    return () => clearTimeout(timer)
  }, [error, clearError])

  const current = room.currentGame
  const entry = GAMES[current.id]
  const Game = entry.component
  const pacing = room.progress
  const ready = pacing && pacing.done >= pacing.goal
  const close = () => setDialog(null)

  const next = async () => {
    close()
    await send('next')
  }

  const release = async (player) => {
    close()
    await send(`players/${player.id}`, undefined, { method: 'DELETE' })
  }

  return (
    <Shell
      middle={room.mode === 'FULL' ? <Trail lineup={room.lineup} /> : <span className="topbar__title">Quick Play</span>}
      actions={
        <>
          {me.isHost && (
            <button type="button" className="btn btn--ghost btn--sm codechip" onClick={() => setDialog('join')} title="Show the QR code">
              <span className="codechip__label">Join</span> {room.code}
            </button>
          )}
          <button type="button" className="btn btn--ghost btn--sm" onClick={() => setDialog('leave')}>
            {me.isHost ? 'Quit' : 'Leave'}
          </button>
        </>
      }
    >
      <div className={`play ${me.isHost ? 'play--host' : 'play--player'}`}>
        <section className="stage panel" aria-label={current.name}>
          <header className="stage__head">
            <GameIcon gameId={current.id} className="stage__icon" />
            <h1 className="stage__name">{current.name}</h1>
            {pacing && <ProgressPill progress={pacing} />}
            <button type="button" className="btn btn--ghost btn--sm stage__rules" onClick={() => setDialog('rules')}>Rules</button>
          </header>
          <div className="stage__body">
            <Game key={current.id} room={room} game={room.game} me={me} send={send} busy={busy} />
          </div>
          {(error || offline) && (
            <p className="alert stage__alert" role="alert">{error || 'Connection lost. Reconnecting…'}</p>
          )}
        </section>

        <aside className="side">
          <HandOut room={room} me={me} send={send} busy={busy} />
          <SipBank room={room} me={me} onRelease={me.isHost ? (player) => setDialog({ release: player }) : null} />
          {me.isHost && (
            <button type="button" className={`btn btn--lg side__next ${ready ? 'btn--primary is-ready' : ''}`}
              onClick={() => setDialog('next')} disabled={busy}>
              {room.nextGame ? `Next: ${room.nextGame}` : 'Finish & see scores'}
            </button>
          )}
        </aside>
      </div>

      <SipToasts players={room.players} me={me} />

      {dialog === 'rules' && (
        <Modal title={`How to play ${current.name}`} onClose={close} wide={Boolean(entry.wideRules)}>
          {entry.Rules ? <entry.Rules game={room.game} /> : (
            <ul className="rules">
              {entry.rules.map((rule) => <li key={rule}>{rule}</li>)}
            </ul>
          )}
        </Modal>
      )}
      {dialog === 'join' && (
        <Modal title="Join from a phone" onClose={close}>
          <JoinCard code={room.code} />
          <p className="modal__note">Mid-game, a phone can take over any seat that's played on this screen.</p>
        </Modal>
      )}
      {dialog === 'next' && (
        <Confirm title={room.nextGame ? `On to ${room.nextGame}?` : 'Finish the night?'} confirmLabel={room.nextGame ? 'Next game' : 'Show the scores'}
          onCancel={close} onConfirm={next}>
          {current.name} ends here and you can't come back to it tonight.
          {!ready && pacing && ` You've played ${pacing.done} of the suggested ${pacing.goal} ${pacing.unit}.`}
        </Confirm>
      )}
      {dialog === 'leave' && (
        <Confirm title={me.isHost ? 'End the night for everyone?' : 'Leave the table?'} confirmLabel={me.isHost ? 'End night' : 'Leave'} danger
          onCancel={close} onConfirm={leave}>
          {me.isHost ? 'The game stops on every screen and the scores are gone.' : 'Your seat stays in the game and goes back to the host screen.'}
        </Confirm>
      )}
      {dialog?.release && (
        <Confirm title={`Play ${dialog.release.name} on this screen?`} confirmLabel="Take over" onCancel={close}
          onConfirm={() => release(dialog.release)}>
          {dialog.release.name}'s phone is disconnected from the seat. They can scan the code again to take it back.
        </Confirm>
      )}
    </Shell>
  )
}
