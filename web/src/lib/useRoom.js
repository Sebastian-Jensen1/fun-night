import { useCallback, useEffect, useMemo, useState } from 'react'
import { api, isGone, watchRoom } from '../api/client'
import { roomStore } from '../api/storage'

const SAFETY_POLL_MS = 10_000

/**
 * Keeps this screen's view of a room up to date: loads it, follows the live-update stream,
 * and sends moves. `status` is 'loading', 'ready' or 'gone' (ended, expired, or this device
 * no longer has a seat).
 */
export function useRoom(roomId) {
  const [creds] = useState(() => roomStore.get(roomId))
  const token = creds?.token ?? null
  const [room, setRoom] = useState(null)
  const [status, setStatus] = useState(token ? 'loading' : 'gone')
  const [offline, setOffline] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState(null)

  // Responses can arrive out of order (a move and a live update racing): never go back in time.
  const apply = useCallback((view) => {
    setRoom((prev) => (!prev || view.version >= prev.version ? view : prev))
    setStatus('ready')
    setOffline(false)
  }, [])

  const gone = useCallback(() => {
    roomStore.remove(roomId)
    setStatus('gone')
  }, [roomId])

  useEffect(() => {
    if (!token) return undefined
    let active = true
    let inFlight = false
    let again = false

    const refresh = async () => {
      if (inFlight) {
        again = true
        return
      }
      inFlight = true
      try {
        do {
          again = false
          const view = await api.getRoom(roomId, token)
          if (!active) return
          apply(view)
        } while (again)
      } catch (err) {
        if (!active) return
        if (isGone(err)) gone()
        else setOffline(true)
      } finally {
        inFlight = false
      }
    }

    const stopWatching = watchRoom(roomId, token, { onPing: refresh, onGone: () => active && gone() })
    const poll = setInterval(refresh, SAFETY_POLL_MS)
    const onVisible = () => document.visibilityState === 'visible' && refresh()
    document.addEventListener('visibilitychange', onVisible)
    refresh()

    return () => {
      active = false
      stopWatching()
      clearInterval(poll)
      document.removeEventListener('visibilitychange', onVisible)
    }
  }, [roomId, token, apply, gone])

  /**
   * Sends a room or game move, e.g. send('blackjack/bet', { playerId, sips }). Resolves to true
   * when the server accepted it. `quiet` moves (automatic ones) don't grey out the buttons.
   */
  const send = useCallback(
    async (action, body, { method = 'POST', quiet = false } = {}) => {
      if (!quiet) setBusy(true)
      setError(null)
      try {
        const view = await api.send(roomId, token, action, body, method)
        if (view) apply(view)
        return true
      } catch (err) {
        if (err.status === 404) gone()
        else if (!quiet) setError(err.message)
        return false
      } finally {
        if (!quiet) setBusy(false)
      }
    },
    [roomId, token, apply, gone],
  )

  const clearError = useCallback(() => setError(null), [])

  const me = useMemo(() => {
    const isHost = room?.you.role === 'HOST'
    const playerId = room?.you.playerId ?? null
    return { isHost, playerId, canActFor: (id) => isHost || id === playerId }
  }, [room?.you.role, room?.you.playerId])

  return { room, status, offline, busy, error, clearError, send, me, token }
}
