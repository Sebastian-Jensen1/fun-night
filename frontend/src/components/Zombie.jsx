import './Zombie.css'

/** A friendly-ish party zombie in a brown suit. Decorative mascot; hosts Brain Dares. */
export default function Zombie({ className = '', cheering = false }) {
  return (
    <svg className={`zombie ${cheering ? 'zombie--cheer' : ''} ${className}`} viewBox="0 0 200 300" aria-hidden="true">
      <g stroke="var(--ink)" strokeWidth="4" strokeLinejoin="round">
        {/* arms reaching forward */}
        <g className="zombie__arm zombie__arm--left">
          <path d="M64 200 Q34 198 14 180 L24 166 Q42 182 66 182 Z" fill="#6b4a2b" />
          <circle cx="16" cy="172" r="12" fill="var(--zombie)" />
          <path d="M8 164 L2 156 M14 160 L12 150 M22 162 L24 152" strokeWidth="5" strokeLinecap="round" />
        </g>
        <g className="zombie__arm zombie__arm--right">
          <path d="M136 200 Q166 198 186 180 L176 166 Q158 182 134 182 Z" fill="#6b4a2b" />
          <circle cx="184" cy="172" r="12" fill="var(--zombie)" />
          <path d="M192 164 L198 156 M186 160 L188 150 M178 162 L176 152" strokeWidth="5" strokeLinecap="round" />
        </g>

        {/* suit */}
        <rect x="88" y="168" width="24" height="20" fill="var(--zombie)" />
        <path d="M48 300 L60 190 Q100 176 140 190 L152 300 Z" fill="#6b4a2b" />
        <path d="M84 184 L100 236 L116 184 Z" fill="#f4f0e0" />
        <path d="M100 192 L92 202 L100 252 L108 202 Z" fill="var(--danger)" />
        <path d="M60 190 L84 184 L94 230 Z M140 190 L116 184 L106 230 Z" fill="#553a20" />

        {/* head */}
        <g className="zombie__head">
          <ellipse cx="100" cy="108" rx="58" ry="64" fill="var(--zombie)" />
          <ellipse cx="72" cy="70" rx="12" ry="8" fill="#7f9c52" stroke="none" />
          <ellipse cx="132" cy="138" rx="9" ry="6" fill="#7f9c52" stroke="none" />
          <path d="M80 48 Q74 26 90 22 M100 46 Q104 20 120 26 M116 50 Q130 36 138 44" fill="none" strokeWidth="5" strokeLinecap="round" />

          {/* mismatched eyes */}
          <circle cx="78" cy="104" r="18" fill="#fffbe0" />
          <circle cx="82" cy="108" r="5" fill="var(--ink)" />
          <circle cx="124" cy="100" r="12" fill="#fffbe0" />
          <circle cx="122" cy="102" r="4" fill="var(--ink)" />
          <path d="M58 82 Q80 74 96 86" fill="none" strokeWidth="7" strokeLinecap="round" />
          <path d="M60 124 Q78 132 94 124" fill="none" stroke="#6f8a45" strokeWidth="4" />

          {/* mouth */}
          <path d="M70 146 Q100 170 132 142 Q120 154 100 156 Q82 154 70 146 Z" fill="#3b1a1a" />
          <path d="M84 150 l4 8 l4 -7 Z M108 152 l4 7 l4 -9 Z" fill="#fffbe0" strokeWidth="2" />
        </g>
      </g>
    </svg>
  )
}
