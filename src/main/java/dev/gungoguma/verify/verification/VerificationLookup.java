package dev.gungoguma.verify.verification;

import java.util.UUID;

public interface VerificationLookup {
    boolean isVerified(UUID uuid);
}
