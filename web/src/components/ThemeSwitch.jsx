import { useTheme } from '../lib/theme'
import { THEMES } from '../lib/themes'

/** Four looks, one tap. Each device remembers its own choice. */
export default function ThemeSwitch() {
  const { theme, setTheme } = useTheme()
  return (
    <div className="themes" role="radiogroup" aria-label="Look">
      {THEMES.map((t) => (
        <button
          key={t.id}
          type="button"
          role="radio"
          aria-checked={theme === t.id}
          aria-label={t.label}
          title={t.label}
          className={`themes__opt themes__opt--${t.id} ${theme === t.id ? 'is-on' : ''}`}
          onClick={() => setTheme(t.id)}
        >
          <span className="themes__swatch" aria-hidden="true" />
          <span className="themes__label">{t.label}</span>
        </button>
      ))}
    </div>
  )
}
