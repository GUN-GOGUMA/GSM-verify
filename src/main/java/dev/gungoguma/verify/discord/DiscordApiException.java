package dev.gungoguma.verify.discord;

import java.io.IOException;

public final class DiscordApiException extends IOException {
    private static final long serialVersionUID = 1L;

    private final int statusCode;
    private final String action;

    public DiscordApiException(String action, int statusCode) {
        super(action + " failed with status " + statusCode);
        this.action = action;
        this.statusCode = statusCode;
    }

    public int statusCode() {
        return statusCode;
    }

    public String action() {
        return action;
    }

    public boolean isUnauthorized() {
        return statusCode == 401 || statusCode == 403;
    }

    public boolean isRateLimited() {
        return statusCode == 429;
    }
}
