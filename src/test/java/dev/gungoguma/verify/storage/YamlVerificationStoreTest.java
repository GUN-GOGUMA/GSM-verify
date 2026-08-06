package dev.gungoguma.verify.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.gungoguma.verify.model.RoleType;
import dev.gungoguma.verify.model.VerifiedUser;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.File;

final class YamlVerificationStoreTest {
    @TempDir
    File tempDir;

    @Test
    void savesFindsAndDeletesVerifiedUser() throws Exception {
        YamlVerificationStore store = new YamlVerificationStore(tempDir);
        UUID uuid = UUID.randomUUID();
        VerifiedUser user = new VerifiedUser(
            uuid,
            "discord",
            "Kim",
            1,
            null,
            RoleType.GRADUATE,
            Instant.parse("2026-01-01T00:00:00Z")
        );

        store.save(user);

        assertTrue(store.isVerified(uuid));
        assertEquals("Kim", store.findByUuid(uuid).orElseThrow().name());
        assertEquals(uuid, store.findByDiscordId("discord").orElseThrow().uuid());

        assertTrue(store.deleteByUuid(uuid));
        assertFalse(store.isVerified(uuid));
    }
}
