package dev.gungoguma.verify.discord;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.gungoguma.verify.config.VerifyConfig;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public final class DiscordApiClient {
    private static final String API_BASE = "https://discord.com/api/v10";

    private final VerifyConfig config;
    private final HttpClient httpClient;
    private final Gson gson;

    public DiscordApiClient(VerifyConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newHttpClient();
        this.gson = new Gson();
    }

    public DiscordUser fetchCurrentUser(String accessToken) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(API_BASE + "/users/@me"))
            .header("Authorization", "Bearer " + accessToken)
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        requireSuccess(response, "Discord user lookup");

        JsonObject json = gson.fromJson(response.body(), JsonObject.class);
        return new DiscordUser(
            json.get("id").getAsString(),
            json.get("username").getAsString(),
            stringOrNull(json, "global_name")
        );
    }

    public DiscordGuildMember fetchGuildMember(String discordId) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(
                URI.create(API_BASE + "/guilds/" + config.guildId() + "/members/" + discordId)
            )
            .header("Authorization", "Bot " + config.discordBotToken())
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 404) {
            return null;
        }
        requireSuccess(response, "Discord guild member lookup");

        JsonObject json = gson.fromJson(response.body(), JsonObject.class);
        return new DiscordGuildMember(stringOrNull(json, "nick"), readRoles(json));
    }

    private void requireSuccess(HttpResponse<String> response, String action) throws DiscordApiException {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new DiscordApiException(action, response.statusCode());
        }
    }

    private List<String> readRoles(JsonObject json) {
        JsonArray rolesJson = json.getAsJsonArray("roles");
        List<String> roles = new ArrayList<>();
        if (rolesJson == null) {
            return roles;
        }

        for (JsonElement role : rolesJson) {
            roles.add(role.getAsString());
        }
        return roles;
    }

    private String stringOrNull(JsonObject json, String key) {
        JsonElement element = json.get(key);
        if (element == null || element.isJsonNull()) {
            return null;
        }

        return element.getAsString();
    }
}
