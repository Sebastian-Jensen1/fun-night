package com.funnight.backend.session;

/** A player in a session. Mutated only while holding the owning session's lock. */
public class Player {
    private final int id;
    private final String name;
    private int sipsDrunk;
    private int sipsGiven;

    public Player(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public int getSipsDrunk() { return sipsDrunk; }
    public int getSipsGiven() { return sipsGiven; }

    public void drink(int sips) { sipsDrunk += sips; }
    public void give(int sips) { sipsGiven += sips; }
}
