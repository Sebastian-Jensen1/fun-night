import { useEffect, useState } from 'react'
import { api } from '../api/client'

/** Loads the game catalog once per mount. */
export function useGames() {
  const [games, setGames] = useState(null)
  const [error, setError] = useState(null)

  useEffect(() => {
    let cancelled = false
    api
      .listGames()
      .then((list) => !cancelled && setGames(list))
      .catch((err) => !cancelled && setError(err.message))
    return () => {
      cancelled = true
    }
  }, [])

  return { games, error }
}
