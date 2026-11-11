import Blackjack from './Blackjack'
import Dares from './Dares'
import HigherLower from './HigherLower'
import RingOfFire, { RingRules } from './RingOfFire'
import Guilty from './Guilty'
import Derby from './Derby'

/**
 * Maps a server game id to the component that plays it and its "how to play" text.
 * Every component receives { room, game, me, send, busy }.
 */
export const GAMES = {
  blackjack: {
    component: Blackjack,
    rules: [
      'Everyone bets 1 to 5 sips against the dealer, then gets two cards.',
      'On your turn: hit for another card, stand to stop, or double down (double your bet, take exactly one card).',
      'Closest to 21 without going over wins. The dealer stands on 17.',
      'Lose and you drink your bet. Win and you get your bet to hand out to others.',
      'A natural blackjack (ace + ten-card) pays 3 to 2.',
    ],
  },
  dares: {
    component: Dares,
    rules: [
      'On your turn, pick a mild or a wild dare.',
      'Do the dare and you win sips to hand out. Chicken out and you drink.',
      'Mild: win 1 or drink 2. Wild: win 3 or drink 4.',
      'The table decides whether a dare really counts as done.',
    ],
  },
  'higher-lower': {
    component: HigherLower,
    rules: [
      'Guess whether the next card is higher or lower than the one showing. Aces are high.',
      'Every right guess adds a card to the pile.',
      'A wrong guess, or the same rank, and you drink one sip per card in the pile (5 at most). The pile resets and the turn moves on.',
      'Three right in a row and you may pass the pile, and the risk, to the next player.',
    ],
  },
  'ring-of-fire': {
    component: RingOfFire,
    Rules: RingRules,
    wideRules: true,
  },
  guilty: {
    component: Guilty,
    rules: [
      'A "who is most likely to..." question comes up.',
      'Everyone votes in secret for the player it fits best. You may vote for yourself.',
      'Whoever gets the most votes is guilty and drinks one sip per vote (5 at most). Ties all drink.',
      'Nobody ever sees who voted for whom.',
      'Sharing one screen? Pass it round: each player taps their name, votes, and hands it on.',
    ],
  },
  derby: {
    component: Derby,
    rules: [
      'The four aces are the horses. Back a suit with a bet of 1 to 5 sips.',
      'Cards are turned one at a time. The ace of that suit moves one step forward.',
      'Along the track lie face-down hurdles. When every horse has passed one, it is turned over and that suit stumbles one step back.',
      'First ace over the line wins. Its backers get double their bet to hand out; everyone else drinks their bet.',
    ],
  },
}
