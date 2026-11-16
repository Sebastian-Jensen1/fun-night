import { useEffect, useState } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { api } from '../api/client'
import { roomStore } from '../api/storage'
import { cleanCode, cleanName, isCode, MAX_NAME_LENGTH, nameError } from '../lib/names'
import Shell from '../components/Shell'

/** Where a phone lands after scanning the QR code: find the night, then take a seat. */
export default function Join({ codeFromLink }) {
  const navigate = useNavigate()
  const linkCode = cleanCode(codeFromLink)
  // Scanning again on a phone that's already seated just goes back to the table
  const [seated] = useState(() => (isCode(linkCode) ? roomStore.findByCode(linkCode) : null))
  const [code, setCode] = useState(linkCode)
  const [found, setFound] = useState(null)
  const [name, setName] = useState('')
  const [touched, setTouched] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState(null)

  useEffect(() => {
    if (!isCode(linkCode) || seated) return undefined
    let active = true
    api
      .lookup(linkCode)
      .then((room) => active && setFound(room))
      .catch((err) => active && setError(err.message))
    return () => {
      active = false
    }
  }, [linkCode, seated])

  if (seated) return <Navigate to={`/room/${seated.roomId}`} replace />

  const findRoom = (e) => {
    e.preventDefault()
    if (isCode(code)) navigate(`/join/${code}`)
  }

  const takeSeat = async (request) => {
    setBusy(true)
    setError(null)
    try {
      const { roomId, token, room } = await api.join({ code: found.code, ...request })
      roomStore.set(roomId, { token, role: 'PLAYER', code: room.code })
      navigate(`/room/${roomId}`, { replace: true })
    } catch (err) {
      setError(err.message)
      setBusy(false)
    }
  }

  const problem = found ? nameError(name, found.seats.map((s) => s.name)) : null
  const openSeats = found ? found.seats.filter((s) => !s.remote) : []
  const inLobby = found?.phase === 'LOBBY'

  const submitName = (e) => {
    e.preventDefault()
    setTouched(true)
    if (!problem) takeSeat({ name: cleanName(name) })
  }

  return (
    <Shell middle={<span className="topbar__title">Join</span>} fit={false}>
      <div className="join">
        {!found && (
          <form className="panel join__card" onSubmit={findRoom}>
            <h1 className="page-title">Join a game night</h1>
            <p className="join__hint">Type the five-character code from the host's screen.</p>
            <input className="input input--code input--big" value={code} onChange={(e) => setCode(cleanCode(e.target.value))}
              placeholder="ABCDE" aria-label="Join code" autoComplete="off" autoCapitalize="characters" spellCheck="false" autoFocus />
            {error && <p className="alert" role="alert">{error}</p>}
            <button type="submit" className="btn btn--primary btn--lg" disabled={!isCode(code)}>Find the table</button>
            <Link to="/" className="btn btn--ghost btn--sm">← Back</Link>
          </form>
        )}

        {found && found.phase === 'FINISHED' && (
          <div className="panel join__card">
            <h1 className="page-title">That night is over</h1>
            <p className="join__hint">Ask the host to start a rematch, then scan again.</p>
            <Link to="/" className="btn btn--primary">Back to start</Link>
          </div>
        )}

        {found && found.phase !== 'FINISHED' && (
          <div className="panel join__card">
            <p className="join__code">Table <strong>{found.code}</strong></p>
            <h1 className="page-title">{inLobby ? 'Take a seat' : 'Which seat is yours?'}</h1>

            {inLobby && !found.full && (
              <form className="join__name" onSubmit={submitName} noValidate>
                <label htmlFor="join-name" className="join__label">Your name</label>
                <input id="join-name" className={`input input--big ${touched && problem ? 'input--error' : ''}`} value={name}
                  onChange={(e) => setName(e.target.value)} maxLength={MAX_NAME_LENGTH} placeholder="e.g. Anna"
                  autoComplete="off" autoFocus aria-invalid={Boolean(touched && problem)} aria-describedby="join-name-error" />
                <span id="join-name-error" className="field-error">{touched && problem ? problem : ''}</span>
                <button type="submit" className="btn btn--primary btn--lg" disabled={busy}>I'm in</button>
              </form>
            )}
            {inLobby && found.full && <p className="join__hint">The table is full (8 players).</p>}
            {!inLobby && <p className="join__hint">The night has already started, so pick the seat the host made for you.</p>}

            {openSeats.length > 0 && (
              <div className="join__seats">
                {inLobby && <p className="join__or">or play a seat the host already added</p>}
                <div className="join__seatlist">
                  {openSeats.map((seat) => (
                    <button key={seat.id} type="button" className="btn" disabled={busy} onClick={() => takeSeat({ seatId: seat.id })}>
                      I'm {seat.name}
                    </button>
                  ))}
                </div>
              </div>
            )}
            {!inLobby && openSeats.length === 0 && <p className="join__hint">Every seat is already on a phone.</p>}

            {error && <p className="alert" role="alert">{error}</p>}
          </div>
        )}
      </div>
    </Shell>
  )
}
