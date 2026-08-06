package dev.gungoguma.verify.verification;

import java.util.UUID;

public final class NoopVerificationLookup implements VerificationLookup {
    @Override
    public boolean isVerified(UUID uuid) {
        return false;
    }
}
