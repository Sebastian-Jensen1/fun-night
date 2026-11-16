// Mirrors the server's name rules so players get instant feedback. The server re-checks everything.
export const MAX_PLAYERS = 8
export const MAX_NAME_LENGTH = 20
const NAME_PATTERN = /^[\p{L}\p{N} _.'!-]+$/u

export const cleanName = (name) => name.trim().replace(/\s+/g, ' ')

/** Returns why `raw` can't be used as a name next to `taken`, or null when it's fine. */
export function nameError(raw, taken = []) {
  const name = cleanName(raw)
  if (!name) return 'Type a name first'
  if (name.length > MAX_NAME_LENGTH) return `Max ${MAX_NAME_LENGTH} characters`
  if (!NAME_PATTERN.test(name)) return "Letters, numbers, spaces and _ . ' ! - only"
  if (taken.some((other) => other.toLowerCase() === name.toLowerCase())) return 'That name is taken'
  return null
}

const CODE_PATTERN = /^[A-Z0-9]{5}$/

/** Join codes are five letters/digits; people type them in any case, with stray spaces. */
export const cleanCode = (raw) => raw.toUpperCase().replace(/[^A-Z0-9]/g, '').slice(0, 5)
export const isCode = (code) => CODE_PATTERN.test(code)
