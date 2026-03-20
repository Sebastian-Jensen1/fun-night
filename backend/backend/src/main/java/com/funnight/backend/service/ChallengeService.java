package com.funnight.backend.service;

import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Service
public class ChallengeService {
    private final List<String> challenges = new ArrayList<>();
    private List<String> availableChallenges = new ArrayList<>();
    private final Random random = new Random();

    public ChallengeService() {
        // Initialize with some default challenges
        challenges.add("Do 10 pushups");
        challenges.add("Tell a joke");
        challenges.add("Sing a song snippet");
        challenges.add("Do a dance move");
        challenges.add("Drink a glass of water");
        
        resetAvailableChallenges();
    }

    private void resetAvailableChallenges() {
        availableChallenges = new ArrayList<>(challenges);
        Collections.shuffle(availableChallenges);
    }

    public synchronized String getRandomChallenge() {
        if (challenges.isEmpty()) {
            return "No challenges available.";
        }

        if (availableChallenges.isEmpty()) {
            resetAvailableChallenges();
        }

        return availableChallenges.remove(random.nextInt(availableChallenges.size()));
    }

    public List<String> getAllChallenges() {
        return new ArrayList<>(challenges);
    }
}
