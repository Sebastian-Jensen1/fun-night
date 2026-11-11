/** Ranks players for the final screen and works out the awards. Ties share an award. */
export function finalStandings(players) {
  const ranked = [...players].sort((a, b) => b.sipsDrunk - a.sipsDrunk || a.name.localeCompare(b.name))
  const mostDrunk = Math.max(0, ...players.map((p) => p.sipsDrunk))
  const mostGiven = Math.max(0, ...players.map((p) => p.sipsGiven))
  let rank = 0
  return {
    totalSips: players.reduce((sum, p) => sum + p.sipsDrunk, 0),
    rows: ranked.map((p, i) => {
      // Players level on sips share a place: 1, 1, 3 rather than 1, 2, 3
      if (i === 0 || p.sipsDrunk !== ranked[i - 1].sipsDrunk) rank = i + 1
      return {
        ...p,
        rank,
        thirstiest: mostDrunk > 0 && p.sipsDrunk === mostDrunk,
        generous: mostGiven > 0 && p.sipsGiven === mostGiven,
        dry: mostDrunk > 0 && p.sipsDrunk === 0,
      }
    }),
  }
}

/** Who drank since the previous view of the room: [{ id, name, sips }]. */
export function sipChanges(before, after) {
  if (!before || !after) return []
  const previous = new Map(before.map((p) => [p.id, p.sipsDrunk]))
  return after
    .filter((p) => previous.has(p.id) && p.sipsDrunk > previous.get(p.id))
    .map((p) => ({ id: p.id, name: p.name, sips: p.sipsDrunk - previous.get(p.id) }))
}
