package com.funnight.backend.controller;

import com.funnight.backend.service.ChallengeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChallengeController {

    private final ChallengeService challengeService;

    @Autowired
    public ChallengeController(ChallengeService challengeService) {
        this.challengeService = challengeService;
    }

    @GetMapping("/api/challenges/random")
    public String challenge(){
        var challenge = challengeService.getRandomChallenge();
        return challenge != null ? challenge.getDescription() : "No challenges available.";
    }
}
