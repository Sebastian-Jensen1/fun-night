import { useEffect, useState } from 'react'
import { api } from '../api/client'
import { isUnreachable, joinUrl, prettyUrl } from '../lib/joinUrl'
import QrCode from './QrCode'

/** The QR code and join code friends use to play from their phones. */
export default function JoinCard({ code, compact = false }) {
  const [lan, setLan] = useState([])

  useEffect(() => {
    let active = true
    api
      .network()
      .then((info) => active && setLan(info.lanAddresses ?? []))
      .catch(() => {})
    return () => {
      active = false
    }
  }, [])

  const url = joinUrl(code, window.location, lan)
  return (
    <div className={`joincard ${compact ? 'joincard--compact' : ''}`}>
      <div className="joincard__qr"><QrCode value={url} /></div>
      <div className="joincard__text">
        <span className="joincard__label">Scan, or go to</span>
        <span className="joincard__url">{prettyUrl(url).replace(`/join/${code}`, '/join')}</span>
        <span className="joincard__label">and enter</span>
        <span className="joincard__code" aria-label={`Join code ${code.split('').join(' ')}`}>{code}</span>
        {isUnreachable(window.location, lan) && (
          <span className="joincard__warn">Phones can't open “localhost”. Open this page on your wifi address instead.</span>
        )}
      </div>
    </div>
  )
}
