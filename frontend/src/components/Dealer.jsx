import './Dealer.css'

const BROWS = {
  idle: ['M88 112 Q104 104 122 110', 'M138 106 Q156 94 174 104'],
  happy: ['M86 108 Q104 96 122 106', 'M138 106 Q156 96 174 108'],
  sad: ['M88 114 Q106 110 122 102', 'M138 102 Q154 110 172 114'],
  shock: ['M86 102 Q104 88 122 100', 'M138 100 Q156 88 174 102'],
}

function Eyes({ mood }) {
  if (mood === 'happy') {
    // squeezed-shut happy eyes
    return (
      <g className="dealer__eyes" fill="none" stroke="var(--ink)" strokeWidth="5" strokeLinecap="round">
        <path d="M92 138 Q106 124 120 138" />
        <path d="M140 138 Q154 124 168 138" />
      </g>
    )
  }
  const pupil = { idle: [0, 2, 7], sad: [0, 6, 6], shock: [0, 0, 4] }[mood] ?? [0, 2, 7]
  const [dx, dy, r] = pupil
  const eyeRy = mood === 'shock' ? 21 : 18
  return (
    <g className="dealer__eyes">
      <ellipse cx="106" cy="134" rx="15" ry={eyeRy} fill="#fff" stroke="var(--ink)" strokeWidth="4" />
      <ellipse cx="154" cy="134" rx="15" ry={eyeRy} fill="#fff" stroke="var(--ink)" strokeWidth="4" />
      <circle cx={108 + dx} cy={136 + dy} r={r} fill="var(--ink)" />
      <circle cx={152 + dx} cy={136 + dy} r={r} fill="var(--ink)" />
      <circle cx={110 + dx} cy={133 + dy} r="2" fill="#fff" />
      <circle cx={154 + dx} cy={133 + dy} r="2" fill="#fff" />
      {mood === 'idle' && (
        // heavy, smug eyelids
        <g fill="#f2c08d" stroke="var(--ink)" strokeWidth="4" strokeLinejoin="round">
          <path d="M90 130 Q106 112 122 130 Z" />
          <path d="M138 130 Q154 112 170 130 Z" />
        </g>
      )}
      <g className="dealer__blink" fill="#f2c08d">
        <ellipse cx="106" cy="134" rx="17" ry="20" />
        <ellipse cx="154" cy="134" rx="17" ry="20" />
      </g>
    </g>
  )
}

function Mouth({ mood }) {
  switch (mood) {
    case 'happy':
      return (
        <g>
          <path d="M106 178 Q130 204 154 178 Z" fill="#7a1f1f" stroke="var(--ink)" strokeWidth="4" strokeLinejoin="round" />
          <path d="M110 180 L150 180 L148 186 L112 186 Z" fill="#fff" />
          <rect x="132" y="180" width="8" height="7" fill="var(--sun)" stroke="var(--ink)" strokeWidth="1.5" />
        </g>
      )
    case 'sad':
      return <path d="M112 192 Q130 178 148 192" fill="none" stroke="var(--ink)" strokeWidth="5" strokeLinecap="round" />
    case 'shock':
      return <ellipse cx="130" cy="188" rx="9" ry="12" fill="#7a1f1f" stroke="var(--ink)" strokeWidth="4" />
    default:
      return <path d="M114 184 Q134 192 152 178" fill="none" stroke="var(--ink)" strokeWidth="5" strokeLinecap="round" />
  }
}

/**
 * "Dealer Dan", the blackjack dealer: a cartoon human with a casino visor, slicked hair and a
 * very confident mustache. Mood changes his face: idle | happy | sad | shock.
 */
export default function Dealer({ mood = 'idle', className = '' }) {
  const [browL, browR] = BROWS[mood] ?? BROWS.idle
  return (
    <svg
      className={`dealer dealer--${mood} ${className}`}
      viewBox="0 0 260 300"
      role="img"
      aria-label={`The dealer looks ${mood === 'idle' ? 'smug' : mood}`}
    >
      <g className="dealer__body" stroke="var(--ink)" strokeWidth="4" strokeLinejoin="round">
        {/* arms */}
        <path d="M76 218 Q42 228 32 262 L54 272 Q62 248 84 240 Z" fill="#fffdf5" />
        <path d="M44 240 L60 248" stroke="var(--danger)" strokeWidth="7" />
        <circle cx="40" cy="274" r="15" fill="#f2c08d" />
        <path d="M184 218 Q218 228 228 262 L206 272 Q198 248 176 240 Z" fill="#fffdf5" />
        <path d="M216 240 L200 248" stroke="var(--danger)" strokeWidth="7" />
        <g className="dealer__cards">
          <rect x="206" y="236" width="26" height="36" rx="4" fill="#fff" transform="rotate(-18 219 254)" />
          <rect x="216" y="234" width="26" height="36" rx="4" fill="#fff" transform="rotate(8 229 252)" />
          <text x="226" y="258" fontSize="14" fill="var(--danger)" stroke="none" fontFamily="var(--font-display)" transform="rotate(8 229 252)">A</text>
        </g>
        <circle cx="220" cy="274" r="15" fill="#f2c08d" />

        {/* neck + torso */}
        <rect x="112" y="184" width="36" height="24" fill="#f2c08d" />
        <path d="M58 300 L72 214 Q130 196 188 214 L202 300 Z" fill="#fffdf5" />
        <path d="M58 300 L72 214 Q100 204 116 206 L130 262 L120 300 Z" fill="#2d2a3a" />
        <path d="M202 300 L188 214 Q160 204 144 206 L130 262 L140 300 Z" fill="#2d2a3a" />
        <circle cx="130" cy="236" r="3" fill="#ccc" strokeWidth="2" />
        <circle cx="130" cy="252" r="3" fill="#ccc" strokeWidth="2" />
        <circle cx="116" cy="282" r="4" fill="var(--sun)" strokeWidth="2" />
        <circle cx="144" cy="282" r="4" fill="var(--sun)" strokeWidth="2" />
        <path d="M130 206 L106 192 L106 220 Z" fill="var(--danger)" />
        <path d="M130 206 L154 192 L154 220 Z" fill="var(--danger)" />
        <circle cx="130" cy="206" r="7" fill="#b02a1a" />
        {/* name tag */}
        <rect x="150" y="226" width="34" height="16" rx="3" fill="var(--sun)" strokeWidth="2.5" />
        <text x="167" y="238" fontSize="10" textAnchor="middle" fill="var(--ink)" stroke="none" fontFamily="var(--font-display)">DAN</text>
      </g>

      <g className="dealer__head">
        <g stroke="var(--ink)" strokeWidth="4">
          <circle cx="64" cy="132" r="14" fill="#f2c08d" />
          <circle cx="196" cy="132" r="14" fill="#f2c08d" />
          <ellipse cx="130" cy="116" rx="66" ry="78" fill="#f2c08d" />
        </g>
        {/* stubble + cheeks */}
        <ellipse cx="130" cy="180" rx="44" ry="16" fill="#b98a5e" opacity=".3" />
        <circle cx="88" cy="160" r="10" fill="#ff8a80" opacity=".45" />
        <circle cx="172" cy="160" r="10" fill="#ff8a80" opacity=".45" />

        {/* slicked hair */}
        <path
          d="M66 104 C58 50 100 30 132 32 C168 32 204 52 194 104 C186 84 170 74 150 76 L132 90 L114 76 C92 74 76 84 66 104 Z"
          fill="#2a1d14" stroke="var(--ink)" strokeWidth="4" strokeLinejoin="round"
        />
        <path d="M94 54 C112 44 142 42 164 50" fill="none" stroke="#6b5646" strokeWidth="5" strokeLinecap="round" />
        <path d="M82 76 C96 60 116 56 128 58 M140 58 C156 58 172 64 182 76" fill="none" stroke="#4a382b" strokeWidth="3" strokeLinecap="round" />

        {/* casino visor */}
        <path d="M66 86 Q130 62 194 86 L194 98 Q130 76 66 98 Z" fill="#2f8a3a" stroke="var(--ink)" strokeWidth="4" strokeLinejoin="round" />
        <path
          d="M56 94 Q130 66 204 94 Q214 108 198 112 Q130 86 62 112 Q46 108 56 94 Z"
          fill="#5fd36a" opacity=".85" stroke="var(--ink)" strokeWidth="4" strokeLinejoin="round"
        />

        <g fill="none" stroke="#2a1d14" strokeWidth="8" strokeLinecap="round">
          <path className="dealer__brow" d={browL} />
          <path className="dealer__brow" d={browR} />
        </g>

        <Eyes mood={mood} />

        {/* nose */}
        <ellipse cx="130" cy="154" rx="17" ry="14" fill="#e8956a" stroke="var(--ink)" strokeWidth="4" />
        <ellipse cx="124" cy="149" rx="5" ry="3" fill="#fff" opacity=".7" />

        <Mouth mood={mood} />

        {/* the mustache */}
        <path
          d="M130 166 C118 160 98 160 90 170 C84 178 74 176 72 168 C72 184 92 190 108 182 C118 178 126 174 130 174 C134 174 142 178 152 182 C168 190 188 184 188 168 C186 176 176 178 170 170 C162 160 142 160 130 166 Z"
          fill="#2a1d14" stroke="var(--ink)" strokeWidth="3" strokeLinejoin="round"
        />

        {mood === 'sad' && (
          <path className="dealer__sweat" d="M186 92 Q196 108 190 114 Q180 114 182 104 Z" fill="#8fd3ff" stroke="var(--ink)" strokeWidth="3" />
        )}
      </g>
    </svg>
  )
}
