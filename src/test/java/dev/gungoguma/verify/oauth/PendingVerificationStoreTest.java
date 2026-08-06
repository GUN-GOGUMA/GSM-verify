package dev.gungoguma.verify.oauth;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class PendingVerificationStoreTest {
    @Test
    void storesPendingVerificationByUuidAndState() {
        PendingVerificationStore store = new PendingVerificationStore(
            Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC),
            300
        );

        PendingVerification pending = store.create(UUID.randomUUID(), "player");

        assertTrue(store.findByUuid(pending.uuid()).isPresent());
        assertTrue(store.findByState(pending.state()).isPresent());
    }

    @Test
    void expiresPendingVerification() {
        UUID uuid = UUID.randomUUID();
        PendingVerificationStore store = new PendingVerificationStore(
            Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC),
            -1
        );

        PendingVerification pending = store.create(uuid, "player");

        assertTrue(store.findByUuid(uuid).isEmpty());
        assertTrue(store.findByState(pending.state()).isEmpty());
    }
}
