import { useState, useEffect } from "react";

function ChallengeData(props) {
    const [challengeData, setChallengeData] = useState({null});
    useEffect(() => {
        fetch("/api/challenges")

        .then(res => res.json())
        .then(data => {
            setChallengeData(data);
        })
    })
}
