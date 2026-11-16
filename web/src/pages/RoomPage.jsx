import { Link } from 'react-router-dom'
import { useRoom } from '../lib/useRoom'
import Shell from '../components/Shell'
import Lobby from './Lobby'
import Play from './Play'
import Final from './Final'

/** One game night on this device: lobby, then the games, then the final scores. */
export default function RoomPage({ roomId }) {
  const state = useRoom(roomId)
  const { room, status, offline } = state

  if (status === 'gone') {
    return (
      <Shell>
        <div className="center">
          <div className="panel notice">
            <h1 className="page-title">This game night has ended</h1>
            <p>It was closed or timed out, or this device no longer has a seat at the table.</p>
            <div className="notice__actions">
              <Link to="/" className="btn btn--primary">Back to start</Link>
              <Link to="/join" className="btn btn--ghost">Join with a code</Link>
            </div>
          </div>
        </div>
      </Shell>
    )
  }

  if (!room) {
    return (
      <Shell>
        <div className="center">
          <p className={offline ? 'alert' : 'loading'} role={offline ? 'alert' : 'status'}>
            {offline ? "Can't reach the game server. Still trying…" : 'Setting up the table…'}
          </p>
        </div>
      </Shell>
    )
  }

  if (room.phase === 'LOBBY') return <Lobby state={state} />
  if (room.phase === 'FINISHED') return <Final state={state} />
  return <Play state={state} />
}
