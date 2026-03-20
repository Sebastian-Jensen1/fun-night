package com.funnight.backend.service;

import com.funnight.backend.model.Challenge;
import org.junit.jupiter.api.Test;
import java.util.HashSet;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

public class ChallengeServiceTest {

    @Test
    public void testRandomChallengesWithoutRepetition() {
        ChallengeService service = new ChallengeService();
        int totalChallenges = service.getAllChallenges().size();
        Set<String> drawnChallenges = new HashSet<>();

        // Draw all challenges exactly once
        for (int i = 0; i < totalChallenges; i++) {
            Challenge challenge = service.getRandomChallenge();
            assertNotNull(challenge);
            assertFalse(drawnChallenges.contains(challenge.getDescription()), "Should not draw the same challenge twice before reset: " + challenge.getDescription());
            drawnChallenges.add(challenge.getDescription());
        }

        assertEquals(totalChallenges, drawnChallenges.size());

        // Draw again, should reset and draw another one
        Challenge nextChallenge = service.getRandomChallenge();
        assertNotNull(nextChallenge);
    }
}
