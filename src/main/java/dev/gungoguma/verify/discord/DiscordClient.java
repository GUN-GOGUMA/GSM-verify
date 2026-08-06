package dev.gungoguma.verify.discord;

public interface DiscordClient {
    DiscordUser fetchCurrentUser(String accessToken) throws Exception;

    DiscordGuildMember fetchGuildMember(String discordId) throws Exception;
}
