import { describe, expect, it } from 'vitest'
import { finalStandings, sipChanges } from './scores'

const player = (id, name, sipsDrunk, sipsGiven = 0) => ({ id, name, sipsDrunk, sipsGiven })

describe('final standings', () => {
  it('ranks by sips drunk and adds up the night', () => {
    const { rows, totalSips } = finalStandings([player(1, 'Anna', 2, 5), player(2, 'Bo', 7), player(3, 'Cy', 0)])
    expect(rows.map((r) => r.name)).toEqual(['Bo', 'Anna', 'Cy'])
    expect(totalSips).toBe(9)
    expect(rows[0].thirstiest).toBe(true)
    expect(rows[1].generous).toBe(true)
    expect(rows[2].dry).toBe(true)
  })

  it('lets tied players share a place and an award', () => {
    const { rows } = finalStandings([player(1, 'Anna', 4, 2), player(2, 'Bo', 4, 2), player(3, 'Cy', 1)])
    expect(rows.map((r) => r.rank)).toEqual([1, 1, 3])
    expect(rows.filter((r) => r.thirstiest).map((r) => r.name)).toEqual(['Anna', 'Bo'])
    expect(rows.filter((r) => r.generous).map((r) => r.name)).toEqual(['Anna', 'Bo'])
  })

  it('hands out no awards when nobody drank or gave anything', () => {
    const { rows } = finalStandings([player(1, 'Anna', 0), player(2, 'Bo', 0)])
    expect(rows.some((r) => r.thirstiest || r.generous || r.dry)).toBe(false)
  })
})

describe('sip changes', () => {
  it('reports who drank since the last view', () => {
    const before = [player(1, 'Anna', 1), player(2, 'Bo', 3)]
    const after = [player(1, 'Anna', 1), player(2, 'Bo', 5)]
    expect(sipChanges(before, after)).toEqual([{ id: 2, name: 'Bo', sips: 2 }])
  })

  it('stays quiet when scores reset for a rematch or a player is new', () => {
    expect(sipChanges([player(1, 'Anna', 6)], [player(1, 'Anna', 0), player(2, 'Bo', 2)])).toEqual([])
    expect(sipChanges(null, [player(1, 'Anna', 2)])).toEqual([])
  })
})
