package dev.gungoguma.verify.discord;

public final class DiscordTokenResponse {
    private final String accessToken;
    private final String tokenType;
    private final int expiresIn;
    private final String scope;

    public DiscordTokenResponse(String accessToken, String tokenType, int expiresIn, String scope) {
        this.accessToken = accessToken;
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
        this.scope = scope;
    }

    public String accessToken() {
        return accessToken;
    }

    public String tokenType() {
        return tokenType;
    }

    public int expiresIn() {
        return expiresIn;
    }

    public String scope() {
        return scope;
    }
}
