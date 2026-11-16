package com.funnight.game;

/** How far a game is towards its suggested length, e.g. 3 of 5 rounds. Purely a pacing hint. */
public record Progress(int done, int goal, String unit) {
}
