package dev.gungoguma.verify.storage;

import dev.gungoguma.verify.model.VerifiedUser;
import dev.gungoguma.verify.verification.VerificationLookup;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

public interface VerificationStore extends VerificationLookup {
    Optional<VerifiedUser> findByUuid(UUID uuid);

    Optional<VerifiedUser> findByDiscordId(String discordId);

    void save(VerifiedUser user) throws IOException;

    boolean deleteByUuid(UUID uuid) throws IOException;

    void deleteAll() throws IOException;
}
