import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { api } from '../api/client'
import { roomStore } from '../api/storage'
import { cleanCode, isCode } from '../lib/names'
import { useCreateRoom } from '../lib/useCreateRoom'
import GameIcon from '../components/GameIcon'
import Shell from '../components/Shell'

const LINE_UP = ['blackjack', 'dares', 'higher-lower', 'ring-of-fire', 'guilty', 'derby']

/** Finds the most recent night this device is still part of, if it's still running. */
function useResumableRoom() {
  const [resumable, setResumable] = useState(null)
  useEffect(() => {
    let active = true
    const [latest] = roomStore.all()
    if (!latest) return undefined
    api
      .getRoom(latest.roomId, latest.token)
      .then((room) => active && setResumable({ roomId: latest.roomId, code: room.code, players: room.players.length }))
      .catch(() => roomStore.remove(latest.roomId))
    return () => {
      active = false
    }
  }, [])
  return resumable
}

export default function Home() {
  const navigate = useNavigate()
  const { create, creating, error } = useCreateRoom()
  const resumable = useResumableRoom()
  const [code, setCode] = useState('')

  const join = (e) => {
    e.preventDefault()
    if (isCode(code)) navigate(`/join/${code}`)
  }

  return (
    <Shell>
      <div className="home">
        <header className="home__hero">
          <p className="home__kicker">Six games · one table · sips on the line</p>
          <h1 className="home__title" aria-label="Stamgæst">
            <span className="home__fun" aria-hidden="true">Stam</span>
            <span className="home__night" aria-hidden="true">gæst</span>
          </h1>
        </header>

        {error && <p className="alert" role="alert">{error}</p>}

        <div className="home__modes">
          <button type="button" className="mode mode--full" onClick={() => create('FULL')} disabled={creating}>
            <span className="mode__tag">The whole thing</span>
            <span className="mode__name">Full Night</span>
            <span className="mode__desc">All six games, back to back, one sip bank.</span>
            <span className="mode__icons" aria-hidden="true">
              {LINE_UP.map((id) => <GameIcon key={id} gameId={id} />)}
            </span>
          </button>
          <Link to="/quick" className="mode mode--quick">
            <span className="mode__tag">Just one</span>
            <span className="mode__name">Quick Play</span>
            <span className="mode__desc">Pick a single game and jump straight in.</span>
            <span className="mode__arrow" aria-hidden="true">→</span>
          </Link>
        </div>

        <div className="home__extras">
          <form className="joinbar panel" onSubmit={join}>
            <label htmlFor="home-code" className="joinbar__label">Got a code?</label>
            <input id="home-code" className="input input--code" value={code} onChange={(e) => setCode(cleanCode(e.target.value))}
              placeholder="ABCDE" autoComplete="off" autoCapitalize="characters" spellCheck="false" inputMode="text" />
            <button type="submit" className="btn" disabled={!isCode(code)}>Join</button>
          </form>
          {resumable && (
            <Link to={`/room/${resumable.roomId}`} className="btn btn--ghost home__resume">
              Back to your night · {resumable.code}
            </Link>
          )}
        </div>

        <p className="home__small">Play responsibly. Water counts as a sip too, and nobody has to drink to play.</p>
      </div>
    </Shell>
  )
}
