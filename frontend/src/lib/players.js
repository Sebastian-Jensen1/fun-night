// Mirrors the backend's validation so players get instant feedback. The server re-checks everything.
export const MIN_PLAYERS = 1
export const MAX_PLAYERS = 8
export const MAX_NAME_LENGTH = 20
const NAME_PATTERN = /^[\p{L}\p{N} _.'!-]+$/u

export const cleanName = (name) => name.trim().replace(/\s+/g, ' ')

/** Returns an error message for each name (or null when it's fine). */
export function validateNames(names) {
  const seen = new Map()
  names.forEach((n, i) => {
    const key = cleanName(n).toLowerCase()
    if (key) seen.set(key, [...(seen.get(key) ?? []), i])
  })
  return names.map((raw) => {
    const name = cleanName(raw)
    if (!name) return 'Every player needs a name'
    if (name.length > MAX_NAME_LENGTH) return `Max ${MAX_NAME_LENGTH} characters`
    if (!NAME_PATTERN.test(name)) return "Letters, numbers, spaces and _ . ' ! - only"
    if (seen.get(name.toLowerCase()).length > 1) return 'Names must be different'
    return null
  })
}
