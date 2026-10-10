/** Small cartoon icon for each game, shown on seed packets and the progress bar. */
export default function GameIcon({ gameId, size = 64 }) {
  const common = { width: size, height: size, viewBox: '0 0 80 80', 'aria-hidden': true }
  const stroke = { stroke: 'var(--ink)', strokeWidth: 4, strokeLinejoin: 'round' }

  if (gameId === 'blackjack') {
    return (
      <svg {...common}>
        <g {...stroke}>
          <rect x="10" y="16" width="34" height="48" rx="6" fill="#fff" transform="rotate(-14 27 40)" />
          <rect x="34" y="12" width="34" height="48" rx="6" fill="#fff" transform="rotate(10 51 36)" />
        </g>
        <text x="14" y="38" fontSize="16" fontFamily="var(--font-display)" fill="var(--ink)" transform="rotate(-14 27 40)">A</text>
        <path d="M52 30 C46 22 38 30 52 44 C66 30 58 22 52 30 Z" fill="var(--danger)" stroke="var(--ink)" strokeWidth="2.5" transform="rotate(10 51 36)" />
      </svg>
    )
  }
  if (gameId === 'dares') {
    return (
      <svg {...common}>
        <g {...stroke}>
          <path d="M18 46 C6 42 8 22 22 22 C24 10 40 8 44 16 C54 8 70 16 66 28 C78 32 74 52 62 52 C60 62 44 64 40 56 C32 64 18 58 18 46 Z" fill="var(--brain)" />
        </g>
        <path d="M28 30 Q34 36 30 42 M44 24 Q40 34 48 38 M56 34 Q52 42 58 46" fill="none" stroke="#c96c80" strokeWidth="3" strokeLinecap="round" />
      </svg>
    )
  }
  if (gameId === 'higher-lower') {
    return (
      <svg {...common}>
        <rect x="22" y="14" width="36" height="52" rx="6" fill="#fff" {...stroke} />
        <text x="40" y="48" textAnchor="middle" fontSize="22" fontFamily="var(--font-display)" fill="var(--ink)">7</text>
        <path d="M66 30 L74 18 L82 30 Z" transform="translate(-6 0)" fill="#8fd14f" {...stroke} strokeWidth="3" />
        <path d="M66 50 L74 62 L82 50 Z" transform="translate(-6 0)" fill="#f0694f" {...stroke} strokeWidth="3" />
      </svg>
    )
  }
  if (gameId === 'ring-of-fire') {
    const cards = Array.from({ length: 10 }, (_, i) => i * 36)
    return (
      <svg {...common}>
        {cards.map((deg) => (
          <rect key={deg} x="36" y="4" width="8" height="14" rx="2" fill="#d9783d" stroke="var(--ink)" strokeWidth="2" transform={`rotate(${deg} 40 40)`} />
        ))}
        <path d="M28 30 h24 l-3 18 a9 9 0 0 1 -18 0 Z" fill="var(--sun)" {...stroke} strokeWidth="3" />
        <path d="M40 57 v8 M32 66 h16" fill="none" {...stroke} strokeWidth="3" strokeLinecap="round" />
        <path d="M33 30 c2 -6 6 -2 7 -8 c1 6 5 2 7 8" fill="#f0694f" stroke="var(--ink)" strokeWidth="2" />
      </svg>
    )
  }
  return (
    <svg {...common}>
      <g {...stroke}>
        <path d="M18 40 L62 40 L56 70 L24 70 Z" fill="#c4632d" />
        <rect x="14" y="34" width="52" height="10" rx="3" fill="#d9783d" />
      </g>
      <text x="40" y="32" textAnchor="middle" fontSize="30" fontFamily="var(--font-display)" fill="var(--white)" stroke="var(--ink)" strokeWidth="2">?</text>
    </svg>
  )
}
