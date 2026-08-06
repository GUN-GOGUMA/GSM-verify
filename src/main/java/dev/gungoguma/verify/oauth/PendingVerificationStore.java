package dev.gungoguma.verify.oauth;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PendingVerificationStore {
    private final Map<String, PendingVerification> byState = new ConcurrentHashMap<>();
    private final Map<UUID, String> stateByUuid = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();
    private final Clock clock;
    private final int expireSeconds;

    public PendingVerificationStore(Clock clock, int expireSeconds) {
        this.clock = clock;
        this.expireSeconds = expireSeconds;
    }

    public PendingVerification create(UUID uuid, String playerName) {
        removeByUuid(uuid);
        Instant now = clock.instant();
        String state = createState();
        PendingVerification pending = new PendingVerification(
            uuid,
            playerName,
            state,
            now,
            now.plusSeconds(expireSeconds)
        );
        byState.put(state, pending);
        stateByUuid.put(uuid, state);
        return pending;
    }

    public Optional<PendingVerification> findByUuid(UUID uuid) {
        String state = stateByUuid.get(uuid);
        if (state == null) {
            return Optional.empty();
        }

        return findByState(state);
    }

    public Optional<PendingVerification> findByState(String state) {
        PendingVerification pending = byState.get(state);
        if (pending == null) {
            return Optional.empty();
        }

        if (pending.isExpired(clock.instant())) {
            remove(pending);
            return Optional.empty();
        }

        return Optional.of(pending);
    }

    public void cleanupExpired() {
        Instant now = clock.instant();
        for (PendingVerification pending : byState.values()) {
            if (pending.isExpired(now)) {
                remove(pending);
            }
        }
    }

    public void remove(PendingVerification pending) {
        byState.remove(pending.state());
        stateByUuid.remove(pending.uuid(), pending.state());
    }

    private void removeByUuid(UUID uuid) {
        String state = stateByUuid.remove(uuid);
        if (state != null) {
            byState.remove(state);
        }
    }

    private String createState() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
