package com.funnight.backend.ringoffire;

import com.funnight.backend.cards.Rank;

/** What happens when a card of each rank is pulled from the ring. */
public enum RingRule {
    WATERFALL(Rank.ACE, "Waterfall", "Everyone starts drinking. You can't stop until the person before you stops.", Target.NONE),
    YOU(Rank.TWO, "You", "Pick someone to drink 2.", Target.OTHER),
    ME(Rank.THREE, "Me", "You drink 2.", Target.NONE),
    FLOOR(Rank.FOUR, "Floor", "Everyone touches the floor. Last one drinks 2.", Target.ANYONE),
    GUYS(Rank.FIVE, "Guys", "All the guys drink.", Target.NONE),
    GIRLS(Rank.SIX, "Girls", "All the girls drink.", Target.NONE),
    HEAVEN(Rank.SEVEN, "Heaven", "Everyone points to the sky. Last one drinks 2.", Target.ANYONE),
    MATE(Rank.EIGHT, "Mate", "Pick a mate. Whenever one of you drinks, so does the other.", Target.OTHER),
    RHYME(Rank.NINE, "Rhyme", "Say a word. Go round rhyming. First to fail drinks 2.", Target.ANYONE),
    CATEGORIES(Rank.TEN, "Categories", "Pick a category. Go round naming things. First to fail drinks 2.", Target.ANYONE),
    RULE(Rank.JACK, "Make a Rule", "Make a rule everyone must follow. Break it, drink.", Target.NONE),
    QUESTION_MASTER(Rank.QUEEN, "Question Master", "You're the Question Master. Anyone who answers your questions drinks.", Target.NONE),
    KINGS_CUP(Rank.KING, "King's Cup", "Pour into the King's Cup. Whoever pulls the 4th king drinks the whole cup!", Target.NONE);

    /** Who the drawer has to pick when resolving the card. */
    public enum Target { NONE, ANYONE, OTHER }

    private final Rank rank;
    private final String title;
    private final String description;
    private final Target target;

    RingRule(Rank rank, String title, String description, Target target) {
        this.rank = rank;
        this.title = title;
        this.description = description;
        this.target = target;
    }

    public Rank getRank() { return rank; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Target getTarget() { return target; }

    public static RingRule of(Rank rank) {
        for (RingRule r : values()) {
            if (r.rank == rank) return r;
        }
        throw new IllegalArgumentException("No rule for " + rank);
    }
}
