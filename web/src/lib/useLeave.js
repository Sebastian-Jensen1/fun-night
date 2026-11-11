import { useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api/client'
import { roomStore } from '../api/storage'

/**
 * Walking away from a night. The host ends it for everyone; a player gives their seat back to
 * the host screen (or leaves the lobby).
 */
export function useLeave({ room, me, token }) {
  const navigate = useNavigate()
  return useCallback(async () => {
    try {
      if (me.isHost) await api.endRoom(room.id, token)
      else await api.send(room.id, token, `players/${me.playerId}`, undefined, 'DELETE')
    } catch {
      // already gone: nothing left to clean up on the server
    }
    roomStore.remove(room.id)
    navigate('/')
  }, [room.id, me.isHost, me.playerId, token, navigate])
}
