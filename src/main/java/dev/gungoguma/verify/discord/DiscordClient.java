package dev.gungoguma.verify.discord;

public interface DiscordClient {
    DiscordUser fetchCurrentUser(String accessToken) throws Exception;

    DiscordGuildMember fetchCurrentUserGuildMember(String accessToken) throws Exception;
}
