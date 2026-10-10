import { useEffect, useRef, useState } from 'react'
import { JazzLoop } from './jazz'

const PREF_KEY = 'funnight:blackjackMusic'

function readPref() {
  try {
    return localStorage.getItem(PREF_KEY) !== 'off'
  } catch {
    return true
  }
}

function savePref(on) {
  try {
    localStorage.setItem(PREF_KEY, on ? 'on' : 'off')
  } catch {
    // storage blocked; the toggle still works for this visit
  }
}

/**
 * Background casino jazz for the blackjack table. On by default, but browsers only allow audio
 * after the user interacts with the page, so it starts on the first tap or click.
 */
export default function MusicToggle() {
  const [on, setOn] = useState(readPref)
  const loopRef = useRef(null)

  useEffect(() => {
    const loop = new JazzLoop()
    loopRef.current = loop
    return () => loop.dispose()
  }, [])

  useEffect(() => {
    const loop = loopRef.current
    if (!on) {
      loop.stop()
      return undefined
    }
    loop.start().catch(() => {})
    // Browsers block audio until the user interacts, so also (re)start on every tap or key press.
    // start() is a no-op once the music is playing.
    const retry = () => loop.start().catch(() => {})
    document.addEventListener('pointerdown', retry)
    document.addEventListener('keydown', retry)
    return () => {
      document.removeEventListener('pointerdown', retry)
      document.removeEventListener('keydown', retry)
    }
  }, [on])

  const toggle = () => {
    setOn((prev) => {
      savePref(!prev)
      return !prev
    })
  }

  return (
    <button
      type="button"
      className={`music-toggle ${on ? 'music-toggle--on' : ''}`}
      onClick={toggle}
      aria-pressed={on}
      title={on ? 'Mute the music' : 'Play some jazz'}
    >
      <span aria-hidden="true">{on ? '🎷' : '🔇'}</span>
      <span className="music-toggle__label">{on ? 'Jazz on' : 'Jazz off'}</span>
      {on && (
        <span className="music-toggle__eq" aria-hidden="true">
          <i /><i /><i />
        </span>
      )}
    </button>
  )
}
