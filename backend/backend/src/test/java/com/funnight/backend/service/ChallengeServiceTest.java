package com.funnight.backend.service;

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
            String challenge = service.getRandomChallenge();
            assertFalse(drawnChallenges.contains(challenge), "Should not draw the same challenge twice before reset: " + challenge);
            drawnChallenges.add(challenge);
        }

        assertEquals(totalChallenges, drawnChallenges.size());

        // Draw again, should reset and draw another one
        String nextChallenge = service.getRandomChallenge();
        assertNotNull(nextChallenge);
    }
}
