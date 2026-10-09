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
        challenges.add(new Challenge("Do your best zombie walk across the room"));
        challenges.add(new Challenge("Talk like a pirate until your next turn"));
        challenges.add(new Challenge("Let the player on your left post a story on your phone"));
        challenges.add(new Challenge("Do an impression of another player until someone guesses who"));
        challenges.add(new Challenge("Hold a plank for 30 seconds"));
        challenges.add(new Challenge("Say the alphabet backwards in under 30 seconds"));
        challenges.add(new Challenge("Give a 30 second dramatic speech about your favourite vegetable"));
        challenges.add(new Challenge("Swap a piece of clothing with the player on your right"));
        challenges.add(new Challenge("Show the last photo in your camera roll"));
        challenges.add(new Challenge("Let the group choose your profile picture for 24 hours"));
        challenges.add(new Challenge("Do 15 squats while everyone counts out loud"));
        challenges.add(new Challenge("Text a friend outside the room only using emojis about tonight"));
        challenges.add(new Challenge("Balance a spoon on your nose for 10 seconds"));
        challenges.add(new Challenge("Compliment every player in the room"));
        challenges.add(new Challenge("Moan like a hungry zombie: BRAAAINS! As loud as you dare"));
        
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
