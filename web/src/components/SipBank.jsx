import { useState } from 'react'

function SeatBadge({ player }) {
  if (!player.remote) return <span className="seat-badge" title="Played on the host screen" aria-label="on the host screen">▣</span>
  return (
    <span className={`seat-badge seat-badge--phone ${player.online ? 'is-online' : 'is-offline'}`}
      title={player.online ? 'Playing from a phone' : 'Phone is offline'}
      aria-label={player.online ? 'on a phone' : 'phone offline'}>
      ▮
    </span>
  )
}

/**
 * Sips won in a game aren't counted until the winner picks who drinks them. This lets every
 * giver this screen controls hand theirs out, one tap per sip.
 */
export function HandOut({ room, me, send, busy }) {
  const givers = room.players.filter((p) => p.pendingGive > 0 && me.canActFor(p.id))
  const [chosen, setChosen] = useState(null)
  if (givers.length === 0 || room.players.length < 2) return null
  const giver = givers.find((p) => p.id === chosen) ?? givers[0]
  const give = (targetId) => send('handout', { playerId: giver.id, targetId, sips: 1 })

  return (
    <section className="handout panel" aria-label="Sips to hand out">
      <h3 className="handout__title">
        <span className="handout__count">{giver.pendingGive}</span>
        {me.playerId === giver.id ? 'sips to hand out' : `${giver.name}: sips to give`}
      </h3>
      {givers.length > 1 && (
        <div className="handout__givers">
          {givers.map((p) => (
            <button key={p.id} type="button" className={`chip chip--btn ${p.id === giver.id ? 'is-on' : ''}`} onClick={() => setChosen(p.id)}>
              {p.name} · {p.pendingGive}
            </button>
          ))}
        </div>
      )}
      <p className="handout__hint">Tap who drinks. One tap, one sip.</p>
      <div className="handout__targets">
        {room.players.filter((p) => p.id !== giver.id).map((p) => (
          <button key={p.id} type="button" className="btn btn--sm" disabled={busy} onClick={() => give(p.id)}>
            {p.name}
          </button>
        ))}
      </div>
    </section>
  )
}

/** The scoreboard: sips drunk and sips handed out for everyone at the table. */
export default function SipBank({ room, me, onRelease }) {
  return (
    <section className="sipbank panel" aria-label="Sip bank">
      <h3 className="sipbank__title">
        Sip Bank
        <span className="sipbank__legend"><span>drunk</span><span>given</span></span>
      </h3>
      <ul className="sipbank__list">
        {room.players.map((p) => (
          <li key={p.id} className={`sipbank__row ${me.playerId === p.id ? 'is-me' : ''}`}>
            <SeatBadge player={p} />
            <span className="sipbank__name">{p.name}</span>
            {p.pendingGive > 0 && <span className="chip chip--give" title="Sips still to hand out">+{p.pendingGive}</span>}
            {onRelease && p.remote && (
              <button type="button" className="sipbank__release" onClick={() => onRelease(p)}
                title={`Take over ${p.name}'s seat on this screen`} aria-label={`Take over ${p.name}'s seat on this screen`}>
                ⇄
              </button>
            )}
            <span className="sipbank__num sipbank__num--drunk">{p.sipsDrunk}</span>
            <span className="sipbank__num sipbank__num--given">{p.sipsGiven}</span>
          </li>
        ))}
      </ul>
    </section>
  )
}
