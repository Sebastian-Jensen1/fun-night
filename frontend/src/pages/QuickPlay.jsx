import { Link, useNavigate } from 'react-router-dom'
import SeedPacket from '../components/SeedPacket'
import { useGames } from '../hooks/useGames'
import './QuickPlay.css'

function QuickPlay() {
  const navigate = useNavigate()
  const { games, error } = useGames()

  return (
    <div className="page">
      <header className="wood-sign">
        <h1 className="outlined page-title">Quick Play</h1>
        <p className="sign-sub">Choose your seed packet</p>
      </header>

      {error && <p className="error-banner" role="alert">{error}</p>}
      {!games && !error && <p className="panel">Digging up the games…</p>}

      {games && (
        <div className="quickplay__grid">
          {games.map((game) => (
            <SeedPacket key={game.id} game={game} onClick={() => navigate(`/setup/quick/${game.id}`)} />
          ))}
        </div>
      )}

      <Link to="/" className="btn btn--wood">◀ Back</Link>
    </div>
  )
}

export default QuickPlay
