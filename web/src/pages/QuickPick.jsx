import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api/client'
import { useCreateRoom } from '../lib/useCreateRoom'
import GameIcon from '../components/GameIcon'
import Shell from '../components/Shell'

export default function QuickPick() {
  const { create, creating, error } = useCreateRoom()
  const [games, setGames] = useState(null)
  const [loadError, setLoadError] = useState(null)

  useEffect(() => {
    let active = true
    api
      .listGames()
      .then((list) => active && setGames(list))
      .catch((err) => active && setLoadError(err.message))
    return () => {
      active = false
    }
  }, [])

  return (
    <Shell middle={<span className="topbar__title">Quick Play</span>}>
      <div className="quick">
        <header className="quick__head">
          <h1 className="page-title">Pick your game</h1>
          <Link to="/" className="btn btn--ghost btn--sm">← Back</Link>
        </header>
        {(error || loadError) && <p className="alert" role="alert">{error || loadError}</p>}
        {!games && !loadError && <p className="quick__loading">Shuffling the deck…</p>}
        {games && (
          <div className="quick__grid">
            {games.map((game) => (
              <button key={game.id} type="button" className="gamecard" onClick={() => create('QUICKPLAY', game.id)} disabled={creating}>
                <span className="gamecard__num">{String(game.order).padStart(2, '0')}</span>
                <GameIcon gameId={game.id} className="gamecard__icon" />
                <span className="gamecard__name">{game.name}</span>
                <span className="gamecard__tag">{game.tagline}</span>
                {game.minPlayers > 1 && <span className="chip gamecard__min">{game.minPlayers}+ players</span>}
              </button>
            ))}
          </div>
        )}
      </div>
    </Shell>
  )
}
