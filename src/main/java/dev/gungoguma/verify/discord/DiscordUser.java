package dev.gungoguma.verify.discord;

public final class DiscordUser {
    private final String id;
    private final String username;
    private final String globalName;

    public DiscordUser(String id, String username, String globalName) {
        this.id = id;
        this.username = username;
        this.globalName = globalName;
    }

    public String id() {
        return id;
    }

    public String username() {
        return username;
    }

    public String globalName() {
        return globalName;
    }
}
