// Session tokens live in sessionStorage: they survive a page refresh but disappear when the
// tab closes. Every access is wrapped because storage can throw (private mode, blocked cookies).

const tokenKey = (sessionId) => `funnight:token:${sessionId}`
const NAMES_KEY = 'funnight:lastPlayers'

function safe(fn, fallback) {
  try {
    return fn()
  } catch {
    return fallback
  }
}

export const tokenStore = {
  get: (sessionId) => safe(() => sessionStorage.getItem(tokenKey(sessionId)), null),
  set: (sessionId, token) => safe(() => sessionStorage.setItem(tokenKey(sessionId), token)),
  remove: (sessionId) => safe(() => sessionStorage.removeItem(tokenKey(sessionId))),
}

/** Remembers the last line-up so the next game night starts pre-filled. */
export const lastPlayers = {
  get: () =>
    safe(() => {
      const names = JSON.parse(localStorage.getItem(NAMES_KEY) ?? '[]')
      return Array.isArray(names) ? names.filter((n) => typeof n === 'string').slice(0, 8) : []
    }, []),
  set: (names) => safe(() => localStorage.setItem(NAMES_KEY, JSON.stringify(names))),
}
