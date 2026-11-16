import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import { themeStore } from '../api/storage'
import { THEMES } from './themes'

const ThemeContext = createContext({ theme: 'synthwave', setTheme: () => {} })

function initialTheme() {
  const saved = themeStore.get()
  return THEMES.some((t) => t.id === saved) ? saved : 'synthwave'
}

/** Each device picks its own look; the choice is remembered on that device. */
export function ThemeProvider({ children }) {
  const [theme, setThemeState] = useState(initialTheme)

  useEffect(() => {
    document.documentElement.dataset.theme = theme
    const meta = document.querySelector('meta[name="theme-color"]')
    if (meta) meta.content = THEMES.find((t) => t.id === theme).color
  }, [theme])

  const setTheme = useCallback((next) => {
    setThemeState(next)
    themeStore.set(next)
  }, [])

  const value = useMemo(() => ({ theme, setTheme }), [theme, setTheme])
  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>
}

// eslint-disable-next-line react-refresh/only-export-components
export const useTheme = () => useContext(ThemeContext)
