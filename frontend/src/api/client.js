// Thin wrapper around fetch for the Fun Night API. Every session-scoped call sends the
// secret session token in a header; the token never goes in the URL so it can't leak via logs.

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? ''
const TOKEN_HEADER = 'X-Session-Token'

export class ApiError extends Error {
  constructor(status, message) {
    super(message)
    this.status = status
  }
}

async function request(path, { method = 'GET', body, token } = {}) {
  const headers = {}
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  if (token) headers[TOKEN_HEADER] = token

  let res
  try {
    res = await fetch(`${BASE_URL}${path}`, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
    })
  } catch {
    throw new ApiError(0, "Can't reach the game server. Is the backend running?")
  }

  if (!res.ok) {
    let detail
    try {
      detail = (await res.json()).detail
    } catch {
      // not JSON, fall through to the generic message
    }
    throw new ApiError(res.status, detail || 'Something went wrong. Try again!')
  }
  return res.status === 204 ? null : res.json()
}

const sessionPath = (id) => `/api/sessions/${encodeURIComponent(id)}`

export const api = {
  listGames: () => request('/api/games'),

  createSession: ({ mode, gameId, playerNames }) =>
    request('/api/sessions', { method: 'POST', body: { mode, gameId, playerNames } }),
  getSession: (id, token) => request(sessionPath(id), { token }),
  nextGame: (id, token) => request(`${sessionPath(id)}/next`, { method: 'POST', token }),
  endSession: (id, token) => request(sessionPath(id), { method: 'DELETE', token }),

  blackjack: {
    get: (id, token) => request(`${sessionPath(id)}/blackjack`, { token }),
    deal: (id, token, bets) =>
      request(`${sessionPath(id)}/blackjack/deal`, { method: 'POST', token, body: { bets } }),
    act: (id, token, playerId, action) =>
      request(`${sessionPath(id)}/blackjack/action`, { method: 'POST', token, body: { playerId, action } }),
  },

  dares: {
    get: (id, token) => request(`${sessionPath(id)}/dares`, { token }),
    draw: (id, token, playerId) =>
      request(`${sessionPath(id)}/dares/draw`, { method: 'POST', token, body: { playerId } }),
    resolve: (id, token, playerId, completed) =>
      request(`${sessionPath(id)}/dares/resolve`, { method: 'POST', token, body: { playerId, completed } }),
  },
}
