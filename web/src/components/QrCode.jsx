import { useMemo } from 'react'
import QRCode from 'qrcode'

const QUIET_ZONE = 3

/**
 * A QR code drawn as an SVG. It is always dark on a light plate, whatever the theme, because
 * phone cameras read that most reliably.
 */
export default function QrCode({ value, className = '' }) {
  const { size, path } = useMemo(() => {
    const { modules } = QRCode.create(value, { errorCorrectionLevel: 'M' })
    let d = ''
    for (let y = 0; y < modules.size; y++) {
      for (let x = 0; x < modules.size; x++) {
        if (modules.data[y * modules.size + x]) d += `M${x + QUIET_ZONE} ${y + QUIET_ZONE}h1v1h-1z`
      }
    }
    return { size: modules.size + QUIET_ZONE * 2, path: d }
  }, [value])

  return (
    <svg className={`qr ${className}`} viewBox={`0 0 ${size} ${size}`} shapeRendering="crispEdges" role="img"
      aria-label="QR code to join this game night">
      <rect width={size} height={size} fill="#fff" />
      <path d={path} fill="#0a0a14" />
    </svg>
  )
}
