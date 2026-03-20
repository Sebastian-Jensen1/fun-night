package com.funnight.backend.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;


@Getter
@NoArgsConstructor(force = true)
@AllArgsConstructor
public class Session {
    private final Long sessionId;
    private List<Player> players = new ArrayList<>();
    private List<Round> rounds = new ArrayList<>();

    public Session(Long sessionId) {
        this.sessionId = sessionId;
        this.players = new ArrayList<>();
        this.rounds = new ArrayList<>();
    }

    public void addPlayer(Player player) {players.add(player);}

    public void removePlayer(Player player) {players.remove(player);}
}
