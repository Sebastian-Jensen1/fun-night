package com.funnight.room;

import java.io.Serializable;

/** A seat at the table. Mutated only while holding the owning room's lock. */
public class Player implements Serializable {
    private final int id;
    private final String name;
    private int sipsDrunk;
    private int sipsGiven;
    private int pendingGive;
    /** Hash of the token of the phone playing this seat, or null when it's played on the host device. */
    private byte[] tokenHash;

    Player(int id, String name, byte[] tokenHash) {
        this.id = id;
        this.name = name;
        this.tokenHash = tokenHash;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public int getSipsDrunk() { return sipsDrunk; }
    public int getSipsGiven() { return sipsGiven; }
    /** Sips this player has won but not yet handed to anyone. */
    public int getPendingGive() { return pendingGive; }
    public boolean isRemote() { return tokenHash != null; }

    byte[] getTokenHash() { return tokenHash; }
    void claim(byte[] tokenHash) { this.tokenHash = tokenHash; }
    void release() { this.tokenHash = null; }

    public void drink(int sips) { sipsDrunk += sips; }

    void addPending(int sips) { pendingGive += sips; }
    void addGiven(int sips) { sipsGiven += sips; }
    void usePending(int sips) { pendingGive -= sips; }

    void resetScore() {
        sipsDrunk = 0;
        sipsGiven = 0;
        pendingGive = 0;
    }
}
