package com.funnight.backend.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@NoArgsConstructor(force = true)
@AllArgsConstructor
public class Player {
    private final Long playerId;
    @Setter
    private String username;
    @Setter
    private int score;

    public Player(Long playerId, String username) {
        this.playerId = playerId;
        this.username = username;
        this.score = 0;
    }
}
