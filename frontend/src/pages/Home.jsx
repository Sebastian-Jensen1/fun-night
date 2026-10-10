import { Link } from 'react-router-dom'
import Dealer from '../components/Dealer'
import Zombie from '../components/Zombie'
import './Home.css'

function Home() {
  return (
    <div className="page home">
      <div className="home__hero">
        <div className="home__dealer">
          <Dealer mood="idle" />
        </div>

        <header className="wood-sign home__title">
          <h1 className="outlined">Fun Night</h1>
          <p className="display home__subtitle">Pints vs. Zombies</p>
        </header>

        <div className="home__zombie">
          <Zombie />
        </div>
      </div>

      <nav className="home__menu" aria-label="Choose how to play">
        <Link to="/setup/full" className="tombstone">
          <span className="tombstone__title display">Full Night</span>
          <span className="tombstone__sub">All 6 games, one after another</span>
        </Link>
        <Link to="/quickplay" className="tombstone tombstone--alt">
          <span className="tombstone__title display">Quick Play</span>
          <span className="tombstone__sub">Pick one game and jump straight in</span>
        </Link>
      </nav>

      <p className="home__footer">
        Play responsibly. Water counts as a sip too 💧 and nobody has to drink to play.
      </p>
    </div>
  )
}

export default Home
