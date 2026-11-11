import { describe, expect, it } from 'vitest'
import { isUnreachable, joinUrl, prettyUrl } from './joinUrl'

const at = (href) => new URL(href)

describe('the link behind the QR code', () => {
  it('uses the page address when the site is hosted', () => {
    expect(joinUrl('ABC23', at('https://funnight.example/room/1'))).toBe('https://funnight.example/join/ABC23')
  })

  it("swaps localhost for the computer's wifi address", () => {
    expect(joinUrl('ABC23', at('http://localhost:5173/room/1'), ['192.168.1.20'])).toBe('http://192.168.1.20:5173/join/ABC23')
  })

  it('keeps a wifi address the host already opened', () => {
    expect(joinUrl('ABC23', at('http://192.168.1.20:5173/'), ['192.168.1.20'])).toBe('http://192.168.1.20:5173/join/ABC23')
  })

  it('warns when phones cannot reach the address', () => {
    expect(isUnreachable(at('http://localhost:5173/'), [])).toBe(true)
    expect(isUnreachable(at('http://localhost:5173/'), ['192.168.1.20'])).toBe(false)
    expect(isUnreachable(at('https://funnight.example/'), [])).toBe(false)
  })

  it('drops the scheme for display', () => {
    expect(prettyUrl('http://192.168.1.20:5173/join/ABC23')).toBe('192.168.1.20:5173/join/ABC23')
  })
})
