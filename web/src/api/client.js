// Thin wrapper around fetch for the Stamgæst API. Room calls send the device's secret token in
// a header; it never goes in the URL, so it can't leak through logs, history or a shared link.

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? ''
const TOKEN_HEADER = 'X-Room-Token'
const UNREACHABLE = "Can't reach the game server. Is it running?"

export class ApiError extends Error {
  constructor(status, message) {
    super(message)
    this.status = status
  }
}

/** True when the room is gone or this device no longer has a seat in it. */
export const isGone = (err) => err instanceof ApiError && (err.status === 403 || err.status === 404)

async function request(path, { method = 'GET', body, token, signal } = {}) {
  const headers = {}
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  if (token) headers[TOKEN_HEADER] = token

  let res
  try {
    res = await fetch(`${BASE_URL}${path}`, {
      method,
      headers,
      signal,
      body: body === undefined ? undefined : JSON.stringify(body),
    })
  } catch (err) {
    if (err.name === 'AbortError') throw err
    throw new ApiError(0, UNREACHABLE)
  }

  if (!res.ok) {
    let detail
    try {
      detail = (await res.json()).detail
    } catch {
      // Not our JSON: a proxy answered because the server behind it is down
      if (res.status >= 500) throw new ApiError(0, UNREACHABLE)
    }
    throw new ApiError(res.status, detail || 'Something went wrong. Try again!')
  }
  return res.status === 204 ? null : res.json()
}

const roomPath = (id) => `/api/rooms/${encodeURIComponent(id)}`

export const api = {
  listGames: () => request('/api/games'),
  network: () => request('/api/network'),

  createRoom: ({ mode, gameId, playerNames }) =>
    request('/api/rooms', { method: 'POST', body: { mode, gameId, playerNames } }),
  lookup: (code) => request(`/api/rooms/lookup?code=${encodeURIComponent(code)}`),
  join: ({ code, name, seatId }) => request('/api/rooms/join', { method: 'POST', body: { code, name, seatId } }),

  getRoom: (id, token, signal) => request(roomPath(id), { token, signal }),
  /** Any room or game move, e.g. send(id, token, 'blackjack/bet', { playerId, sips }). Returns the new room view. */
  send: (id, token, action, body, method = 'POST') =>
    request(`${roomPath(id)}/${action}`, { method, token, body: body ?? (method === 'POST' ? {} : undefined) }),
  endRoom: (id, token) => request(roomPath(id), { method: 'DELETE', token }),
}

/**
 * Opens the room's live-update stream and calls onPing whenever the room changed. Uses fetch
 * rather than EventSource so the token can travel in a header. Reconnects on its own.
 * Returns a function that closes the stream.
 */
export function watchRoom(id, token, { onPing, onGone }) {
  const controller = new AbortController()
  let stopped = false
  let retryMs = 1000

  const connect = async () => {
    while (!stopped) {
      try {
        const res = await fetch(`${BASE_URL}${roomPath(id)}/events`, {
          headers: { [TOKEN_HEADER]: token, Accept: 'text/event-stream' },
          signal: controller.signal,
        })
        if (res.status === 403 || res.status === 404) {
          onGone()
          return
        }
        if (!res.ok || !res.body) throw new Error('stream unavailable')
        retryMs = 1000
        const reader = res.body.getReader()
        const decoder = new TextDecoder()
        let buffer = ''
        for (;;) {
          const { done, value } = await reader.read()
          if (done) break
          buffer += decoder.decode(value, { stream: true })
          // Events are separated by a blank line; comment lines (heartbeats) carry no data
          const events = buffer.split(/\r?\n\r?\n/)
          buffer = events.pop()
          for (const event of events) {
            if (/^event:\s*closed/m.test(event)) {
              onGone()
              return
            }
            if (/^data:/m.test(event)) onPing()
          }
        }
      } catch {
        if (stopped) return
      }
      // Dropped (phone slept, wifi blip, server restart): catch up, then reconnect
      if (!stopped) {
        onPing()
        await new Promise((resolve) => setTimeout(resolve, retryMs))
        retryMs = Math.min(retryMs * 2, 8000)
      }
    }
  }
  connect()

  return () => {
    stopped = true
    controller.abort()
  }
}
