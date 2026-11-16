const LOCAL_HOSTS = new Set(['localhost', '127.0.0.1', '[::1]', '::1'])

/**
 * The link behind the QR code. A phone can't open "localhost", so when the host screen was
 * opened that way the link uses the computer's wifi address instead (as reported by the server).
 */
export function joinUrl(code, location, lanAddresses = []) {
  const local = LOCAL_HOSTS.has(location.hostname)
  if (local && lanAddresses.length > 0) {
    const port = location.port ? `:${location.port}` : ''
    return `${location.protocol}//${lanAddresses[0]}${port}/join/${code}`
  }
  return `${location.origin}/join/${code}`
}

/** True when friends can't reach this address from their phones. */
export function isUnreachable(location, lanAddresses = []) {
  return LOCAL_HOSTS.has(location.hostname) && lanAddresses.length === 0
}

/** The address without the scheme, for showing under the QR code. */
export const prettyUrl = (url) => url.replace(/^https?:\/\//, '')
