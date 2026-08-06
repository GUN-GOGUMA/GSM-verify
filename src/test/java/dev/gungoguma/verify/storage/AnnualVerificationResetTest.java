package dev.gungoguma.verify.storage;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.gungoguma.verify.TestConfigs;
import dev.gungoguma.verify.model.RoleType;
import dev.gungoguma.verify.model.VerifiedUser;
import java.io.File;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class AnnualVerificationResetTest {
    @TempDir
    File tempDir;

    @Test
    void resetsOnceOnOrAfterResetDate() throws Exception {
        YamlVerificationStore store = new YamlVerificationStore(tempDir);
        store.save(new VerifiedUser(
            UUID.randomUUID(),
            "discord",
            "Kim",
            null,
            "1234",
            RoleType.STUDENT,
            Instant.parse("2026-01-01T00:00:00Z")
        ));
        AnnualVerificationReset reset = new AnnualVerificationReset(tempDir, TestConfigs.verifyConfig(), store);

        assertFalse(reset.resetIfNeeded(LocalDate.of(2026, 1, 11)));
        assertTrue(reset.resetIfNeeded(LocalDate.of(2026, 1, 12)));
        assertFalse(reset.resetIfNeeded(LocalDate.of(2026, 1, 13)));
    }
}
