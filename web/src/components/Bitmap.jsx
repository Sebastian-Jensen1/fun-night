/** Draws a tiny pixel-art shape from rows of '#' and '.' as one crisp SVG path. */
export default function Bitmap({ rows, className, title }) {
  const width = rows[0].length
  let d = ''
  rows.forEach((row, y) => {
    let x = 0
    while (x < width) {
      if (row[x] !== '#') {
        x++
        continue
      }
      let run = 1
      while (row[x + run] === '#') run++
      d += `M${x} ${y}h${run}v1h-${run}z`
      x += run
    }
  })
  return (
    <svg
      className={className}
      viewBox={`0 0 ${width} ${rows.length}`}
      shapeRendering="crispEdges"
      fill="currentColor"
      role={title ? 'img' : undefined}
      aria-label={title}
      aria-hidden={title ? undefined : true}
    >
      <path d={d} />
    </svg>
  )
}
