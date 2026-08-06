package dev.gungoguma.verify.discord;

import dev.gungoguma.verify.model.VerifiedUser;

public final class DiscordVerificationResult {
    private final VerifiedUser user;
    private final String failureMessage;

    private DiscordVerificationResult(VerifiedUser user, String failureMessage) {
        this.user = user;
        this.failureMessage = failureMessage;
    }

    public static DiscordVerificationResult success(VerifiedUser user) {
        return new DiscordVerificationResult(user, null);
    }

    public static DiscordVerificationResult failure(String message) {
        return new DiscordVerificationResult(null, message);
    }

    public boolean success() {
        return user != null;
    }

    public VerifiedUser user() {
        return user;
    }

    public String failureMessage() {
        return failureMessage;
    }
}
