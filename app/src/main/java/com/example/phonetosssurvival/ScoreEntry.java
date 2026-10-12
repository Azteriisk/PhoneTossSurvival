package com.example.phonetosssurvival;

public class ScoreEntry {
    private final long id;
    private final String playerName;
    private final float magnitude;
    private final float gForce;
    private final String timestamp;

    public ScoreEntry(long id, String playerName, float magnitude, float gForce, String timestamp) {
        this.id = id;
        this.playerName = (playerName == null || playerName.trim().isEmpty()) ? "ANONYMOUS" : playerName.trim();
        this.magnitude = magnitude;
        this.gForce = gForce;
        this.timestamp = timestamp;
    }

    public ScoreEntry(String playerName, float magnitude, float gForce, String timestamp) {
        this(-1, playerName, magnitude, gForce, timestamp);
    }

    public long getId() {
        return id;
    }

    public String getPlayerName() {
        return playerName;
    }

    public float getMagnitude() {
        return magnitude;
    }

    public float getGForce() {
        return gForce;
    }

    public String getTimestamp() {
        return timestamp;
    }
}
