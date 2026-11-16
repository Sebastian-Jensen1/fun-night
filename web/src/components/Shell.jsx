import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import ThemeSwitch from './ThemeSwitch'

function FullscreenButton() {
  const [on, setOn] = useState(false)

  useEffect(() => {
    const update = () => setOn(Boolean(document.fullscreenElement))
    document.addEventListener('fullscreenchange', update)
    return () => document.removeEventListener('fullscreenchange', update)
  }, [])

  if (!document.documentElement.requestFullscreen) return null
  const toggle = () => {
    if (document.fullscreenElement) document.exitFullscreen()
    else document.documentElement.requestFullscreen().catch(() => {})
  }
  return (
    <button type="button" className="btn btn--ghost btn--icon topbar__full" onClick={toggle}
      aria-label={on ? 'Leave full screen' : 'Full screen'} title={on ? 'Leave full screen' : 'Full screen'}>
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="square" aria-hidden="true">
        {on ? <path d="M9 4v5H4M15 4v5h5M9 20v-5H4M15 20v-5h5" /> : <path d="M4 9V4h5M20 9V4h-5M4 15v5h5M20 15v5h-5" />}
      </svg>
    </button>
  )
}

/** The frame every page sits in: brand on the left, page-specific middle, look switcher on the right. */
export default function Shell({ middle, actions, fit = true, children }) {
  return (
    <div className={`shell ${fit ? 'shell--fit' : ''}`}>
      <header className="topbar">
        <Link to="/" className="brand" aria-label="Stamgæst, back to start">
          <span className="brand__fun">Stam</span>
          <span className="brand__night">gæst</span>
        </Link>
        <div className="topbar__middle">{middle}</div>
        <div className="topbar__actions">
          {actions}
          <ThemeSwitch />
          <FullscreenButton />
        </div>
      </header>
      <main className="shell__main">{children}</main>
    </div>
  )
}
