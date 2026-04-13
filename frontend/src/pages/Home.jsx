import React from 'react';
import './Home.css';
import { useSessionHook } from '../hooks/useSession.js';
import { useNavigate } from 'react-router-dom'

function Home() {
    const { error, createSession } = useSessionHook();
    const navigate = useNavigate(); //used to insert on a button to navigate to a page

    const handleStartGame = async () => {
        try {
            const newSession = await createSession();
            navigate(`/create-session/${newSession.id}`);
        } catch (err) {
            console.error("Error creating session:", err);
        }
    };

    if (error) return <div>Error Occurred: {error}</div>;

    return (
        <div className={"home-container"} >
            <h1 className={"welcome-home"}>Welcome!</h1>

            <button className={"start-game-button"} onClick={handleStartGame}>
                Start Game
            </button>

        </div>
    );
}

export default Home;