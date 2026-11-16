import { useCallback, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api/client'
import { roomStore } from '../api/storage'

/** Opens a lobby on this device (which becomes the host) and goes to it. */
export function useCreateRoom() {
  const navigate = useNavigate()
  const [creating, setCreating] = useState(false)
  const [error, setError] = useState(null)

  const create = useCallback(
    async (mode, gameId = null) => {
      setCreating(true)
      setError(null)
      try {
        const { roomId, token, room } = await api.createRoom({ mode, gameId, playerNames: [] })
        roomStore.set(roomId, { token, role: 'HOST', code: room.code })
        navigate(`/room/${roomId}`)
      } catch (err) {
        setError(err.message)
        setCreating(false)
      }
    },
    [navigate],
  )

  return { create, creating, error }
}
