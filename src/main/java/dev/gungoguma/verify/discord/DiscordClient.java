package dev.gungoguma.verify.discord;

import java.util.List;

public interface DiscordClient {
    DiscordUser fetchCurrentUser(String accessToken) throws Exception;

    List<DiscordGuild> fetchCurrentUserGuilds(String accessToken) throws Exception;

    DiscordGuildMember fetchCurrentUserGuildMember(String accessToken) throws Exception;
}
