package dev.gungoguma.verify.discord;

import java.util.List;

public final class DiscordGuildMember {
    private final String nick;
    private final List<String> roles;

    public DiscordGuildMember(String nick, List<String> roles) {
        this.nick = nick;
        this.roles = List.copyOf(roles);
    }

    public String nick() {
        return nick;
    }

    public List<String> roles() {
        return roles;
    }
}
