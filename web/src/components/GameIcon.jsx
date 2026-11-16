const VECTOR = {
  blackjack: (
    <>
      <rect x="3" y="5.5" width="10.5" height="14.5" rx="2" transform="rotate(-9 8.2 12.7)" />
      <rect x="10.5" y="4" width="10.5" height="14.5" rx="2" transform="rotate(9 15.7 11.2)" fill="var(--icon-fill, none)" />
      <path d="M15.8 8.300c-1.6 1.7-2.5 2.6-2.5 3.700a1.4 1.4 0 0 0 2.2 1.100l-.3 1.300h1.300l-.3-1.300a1.4 1.4 0 0 0 2.2-1.100c0-1.1-1-2-2.6-3.700z" fill="currentColor" stroke="none" transform="rotate(9 15.7 11.2)" />
    </>
  ),
  dares: <path d="M13.5 2 4.5 13.500h6L9 22l10-12.500h-6.200z" />,
  'higher-lower': <path d="M7.5 20V5M3 9.5 7.5 5 12 9.500M16.5 4v15M12 14.500l4.5 4.5 4.5-4.5" />,
  'ring-of-fire': (
    <>
      <path d="M12 2.500c.6 3.6 4.5 5 4.5 9.300a4.5 4.5 0 0 1-9 0c0-1.7.8-2.8 1.7-3.7.1 1.7.8 2.6 1.8 2.7-.3-2.9-.7-4.8 1-8.300z" />
      <ellipse cx="12" cy="18.5" rx="9" ry="3.2" />
    </>
  ),
  guilty: (
    <>
      <rect x="11.2" y="3.2" width="8.5" height="5" rx="1" transform="rotate(45 15.4 5.7)" />
      <path d="M12.3 9.7 4 18M12.5 21h8.5" />
    </>
  ),
  derby: <path d="M5 21.500V3M5 4h14l-3.2 4.200L19 12.500H5M9.7 4v8.500M14.3 4v8.500M5 8.200h10.8" />,
}

/** The symbol for a game, drawn as a line icon that takes the theme's colours. */
export default function GameIcon({ gameId, className = '' }) {
  return (
    <svg className={`gicon ${className}`} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7"
      strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      {VECTOR[gameId] ?? VECTOR.dares}
    </svg>
  )
}
