package com.funnight.backend.service;

import com.funnight.backend.exception.NoChallengesAvailableException;
import com.funnight.backend.model.Challenge;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Service
public class ChallengeService {
    private final List<Challenge> challenges = new ArrayList<>();
    private List<Challenge> availableChallenges = new ArrayList<>();
    private final Random random = new Random();

    public ChallengeService() {
        // Initialize with some default challenges
        challenges.add(new Challenge("Do 10 pushups"));
        challenges.add(new Challenge("Tell a joke"));
        challenges.add(new Challenge("Sing a song snippet"));
        challenges.add(new Challenge("Do a dance move"));
        challenges.add(new Challenge("Drink a glass of water"));
        
        resetAvailableChallenges();
    }

    private void resetAvailableChallenges() {
        availableChallenges = new ArrayList<>(challenges);
        Collections.shuffle(availableChallenges);
    }
    /** Synchronized does so only 1 player can draw at a time, creates a queue*/
    public synchronized Challenge getRandomChallenge() {
        if (challenges.isEmpty()) {
            throw new NoChallengesAvailableException();
        }

        if (availableChallenges.isEmpty()) {
            resetAvailableChallenges();
        }

        return availableChallenges.remove(random.nextInt(availableChallenges.size()));
    }

    public List<Challenge> getAllChallenges() {
        return new ArrayList<>(challenges);
    }
}
