package com.funnight.backend.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.Map;

@Getter
@NoArgsConstructor(force = true)
@AllArgsConstructor
public class Round {
    private Long roundId;
    private Map<Long,Challenge> playerChallenges;
}
