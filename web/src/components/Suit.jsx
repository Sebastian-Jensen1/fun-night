import { isRed } from '../lib/suits'

const VECTOR = {
  HEARTS: <path d="M12 21s-8-5.2-8-11.2A4.8 4.8 0 0 1 12 6.6a4.8 4.8 0 0 1 8 3.2C20 15.8 12 21 12 21z" />,
  DIAMONDS: <path d="M12 2l7.5 10L12 22 4.5 12z" />,
  SPADES: <path d="M12 2C8.5 6.5 4 9 4 13.2a4.3 4.3 0 0 0 6.9 3.4L10 21h4l-.9-4.4a4.3 4.3 0 0 0 6.9-3.4C20 9 15.5 6.5 12 2z" />,
  CLUBS: (
    <>
      <circle cx="12" cy="7" r="4.2" />
      <circle cx="7" cy="13.6" r="4.2" />
      <circle cx="17" cy="13.6" r="4.2" />
      <path d="M10.6 12h2.8l1.1 9h-5z" />
    </>
  ),
}

/** A suit symbol drawn as a shape, so it looks the same on every device and matches the theme. */
export default function Suit({ suit, className = '' }) {
  const classes = `suit suit--${isRed(suit) ? 'red' : 'black'} ${className}`
  return (
    <svg className={classes} viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
      {VECTOR[suit]}
    </svg>
  )
}
