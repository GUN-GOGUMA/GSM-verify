package dev.gungoguma.verify.oauth;

import java.time.Instant;
import java.util.UUID;

public final class PendingVerification {
    private final UUID uuid;
    private final String playerName;
    private final String state;
    private final Instant createdAt;
    private final Instant expiresAt;

    public PendingVerification(UUID uuid, String playerName, String state, Instant createdAt, Instant expiresAt) {
        this.uuid = uuid;
        this.playerName = playerName;
        this.state = state;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public UUID uuid() {
        return uuid;
    }

    public String playerName() {
        return playerName;
    }

    public String state() {
        return state;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }
}
