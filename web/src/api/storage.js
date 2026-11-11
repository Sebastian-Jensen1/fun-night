// What this device remembers between visits. Tokens live in localStorage so closing the tab (or
// a phone putting the browser to sleep) doesn't lose the night; they're dropped after a day.
// Every access is wrapped because storage can throw (private mode, blocked cookies).

const ROOMS_KEY = 'funnight:rooms'
const CREW_KEY = 'funnight:crew'
const THEME_KEY = 'funnight:theme'
const MAX_AGE_MS = 24 * 60 * 60 * 1000

function read(key, fallback) {
  try {
    const value = JSON.parse(localStorage.getItem(key))
    return value ?? fallback
  } catch {
    return fallback
  }
}

function write(key, value) {
  try {
    localStorage.setItem(key, JSON.stringify(value))
  } catch {
    // storage unavailable: the app still works, it just forgets on reload
  }
}

function freshRooms() {
  const rooms = read(ROOMS_KEY, {})
  const now = Date.now()
  return Object.fromEntries(
    Object.entries(rooms && typeof rooms === 'object' ? rooms : {}).filter(
      ([, r]) => r && typeof r.token === 'string' && now - (r.savedAt ?? 0) < MAX_AGE_MS,
    ),
  )
}

/** The rooms this device holds a token for: { [roomId]: { token, role, code, savedAt } }. */
export const roomStore = {
  get: (roomId) => freshRooms()[roomId] ?? null,
  set: (roomId, { token, role, code }) => write(ROOMS_KEY, { ...freshRooms(), [roomId]: { token, role, code, savedAt: Date.now() } }),
  remove: (roomId) => {
    const rooms = freshRooms()
    delete rooms[roomId]
    write(ROOMS_KEY, rooms)
  },
  findByCode: (code) => {
    const entry = Object.entries(freshRooms()).find(([, r]) => r.code === code)
    return entry ? { roomId: entry[0], ...entry[1] } : null
  },
  /** Most recently saved first. */
  all: () =>
    Object.entries(freshRooms())
      .map(([roomId, r]) => ({ roomId, ...r }))
      .sort((a, b) => b.savedAt - a.savedAt),
}

/** Remembers the last line-up typed on this device so the next night starts with one tap. */
export const lastCrew = {
  get: () => {
    const names = read(CREW_KEY, [])
    return Array.isArray(names) ? names.filter((n) => typeof n === 'string').slice(0, 8) : []
  },
  set: (names) => write(CREW_KEY, names),
}

export const themeStore = {
  get: () => read(THEME_KEY, null),
  set: (theme) => write(THEME_KEY, theme),
}
