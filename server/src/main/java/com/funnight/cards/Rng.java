package com.funnight.cards;

import java.security.SecureRandom;

/** One shared cryptographically secure RNG, so shuffles and draws can't be predicted. */
public final class Rng {
    public static final SecureRandom SECURE = new SecureRandom();

    private Rng() {
    }
}
