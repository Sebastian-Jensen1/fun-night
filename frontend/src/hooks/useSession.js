import { useState, useEffect } from "react";

export function useSessionHook() {
    const [sessions, setSessions] = useState([]);
    const [error, setError] = useState(null);

    // Funktion til at hente alle sessioner
    const fetchSessions = () => {
        fetch("/api/session/all")
            .then((res) => {
                if (!res.ok) throw new Error("Sessions loaded failed.");
                return res.json();
            })
            .then((data) => {
                setSessions(data);
            })
            .catch((err) => {
                console.error("Error:", err);
                setError(err.message);
            });
    };

    // Hent sessioner når hook'en indlæses
    useEffect(() => {
        fetchSessions();
    }, []);

    // Funktion til at oprette en ny session
    const createSession = () => {
        return fetch("/api/session/create", {
            method: "POST",
        })
            .then((res) => {
                if (!res.ok) throw new Error("Could not create session.");
                return res.json();
            })
            .then((newSession) => {
                // Opdater listen med den nye session uden at hente det hele igen,
                // eller kald fetchSessions() for at være helt sikker på synkronisering.
                fetchSessions();
                return newSession;
            })
            .catch((err) => {
                console.error("Error creating session:", err);
                setError(err.message);
            });
    };

    return { sessions, error, createSession };
}
