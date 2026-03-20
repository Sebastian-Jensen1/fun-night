package com.funnight.backend.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor(force = true)
@AllArgsConstructor
public class Round {
    private Long RoundId;
    private List<Player> players;
    private List<Challenge> challenges;
}
