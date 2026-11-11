import { useEffect } from 'react'

/** Keeps the screen on while a game is showing, where the browser supports it. */
export function useWakeLock(active) {
  useEffect(() => {
    if (!active || !('wakeLock' in navigator)) return undefined
    let lock = null
    let released = false

    const acquire = async () => {
      try {
        lock = await navigator.wakeLock.request('screen')
        if (released) lock.release()
      } catch {
        // denied (battery saver, not visible): nothing to do
      }
    }
    // The lock is dropped whenever the tab is hidden, so take it again when it comes back
    const onVisible = () => document.visibilityState === 'visible' && acquire()
    document.addEventListener('visibilitychange', onVisible)
    acquire()

    return () => {
      released = true
      document.removeEventListener('visibilitychange', onVisible)
      lock?.release().catch(() => {})
    }
  }, [active])
}
