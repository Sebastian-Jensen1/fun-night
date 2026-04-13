import { useState, useEffect } from "react";

export function usePlayersHook(){
    const [players, setPlayers] = useState([]);
    const [error, setError] = useState(null);

    const addPlayer = (username) => {
        return fetch("/api/{id}/player", {
            method: "POST",
            body: username,
        })
            .then((res) => {
                if (!res.ok) throw new Error("Could not add player.");
                return res.json();
            })
            .then((newPlayer) => {
                setPlayers(newPlayer);
                return newPlayer;
            })
            .catch((err) => {
                console.error("Error adding player:", err);
                setError(err.message);
            });
    };
    return {players, error, addPlayer};
}