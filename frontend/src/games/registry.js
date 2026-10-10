import Blackjack from './blackjack/Blackjack'
import Dares from './dares/Dares'
import HigherLower from './higherlower/HigherLower'
import RingOfFire from './ringoffire/RingOfFire'

/**
 * Maps a backend game id to the component that plays it. To add game 5–6: build the
 * component, register it here, and mark the game available in the backend's GameType enum.
 */
export const GAME_COMPONENTS = {
  blackjack: Blackjack,
  dares: Dares,
  'higher-lower': HigherLower,
  'ring-of-fire': RingOfFire,
}
