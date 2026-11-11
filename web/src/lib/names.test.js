import { describe, expect, it } from 'vitest'
import { cleanCode, cleanName, isCode, nameError } from './names'

describe('player names', () => {
  it('trims and collapses whitespace', () => {
    expect(cleanName('  Anna   Marie ')).toBe('Anna Marie')
  })

  it('accepts ordinary names, including Danish letters', () => {
    expect(nameError('Søren')).toBeNull()
    expect(nameError("O'Neil")).toBeNull()
    expect(nameError('Hanne-Marie 2')).toBeNull()
  })

  it('rejects empty, too long and unsafe names', () => {
    expect(nameError('   ')).toMatch(/Type a name/)
    expect(nameError('x'.repeat(21))).toMatch(/Max 20/)
    expect(nameError('<b>Bo</b>')).toMatch(/only/)
  })

  it('rejects a name someone already has, whatever the case', () => {
    expect(nameError('anna', ['Anna', 'Bo'])).toMatch(/taken/)
    expect(nameError('Cy', ['Anna', 'Bo'])).toBeNull()
  })
})

describe('join codes', () => {
  it('normalises what people type', () => {
    expect(cleanCode(' ab-c2 3x')).toBe('ABC23')
    expect(isCode('ABC23')).toBe(true)
    expect(isCode('ABC2')).toBe(false)
  })
})
