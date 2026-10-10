import Blackjack from './blackjack/Blackjack'
import Dares from './dares/Dares'

/**
 * Maps a backend game id to the component that plays it. To add game 3–6: build the
 * component, register it here, and mark the game available in the backend's GameType enum.
 */
export const GAME_COMPONENTS = {
  blackjack: Blackjack,
  dares: Dares,
}
