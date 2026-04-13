import React, { useState } from 'react';
import { useParams } from 'react-router-dom';
import { usePlayersHook } from '../hooks/usePlayers';

function Session() {
    const { id } = useParams();
    const { addPlayer } = usePlayersHook();
    const [playerName, setPlayerName] = useState("");

    const handleAddPlayer = () => {
        addPlayer(id, playerName);
    };

    return (
        <div>
            <h1>Add Players</h1>

            <input
                type={"text"}
                value={playerName}
                onChange={(e) => setPlayerName(e.target.value)}
                placeholder={"Write name"}
            />

            <button onClick={handleAddPlayer}>
                Add Player
            </button>

            <p>Players in Session: {playerName}</p>
        </div>
    );
}

export default Session;