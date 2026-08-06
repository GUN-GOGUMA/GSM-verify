package dev.gungoguma.verify.discord;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dev.gungoguma.verify.config.VerifyConfig;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public final class DiscordOAuthClient {
    private static final URI TOKEN_URI = URI.create("https://discord.com/api/oauth2/token");

    private final VerifyConfig config;
    private final HttpClient httpClient;
    private final Gson gson;

    public DiscordOAuthClient(VerifyConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newHttpClient();
        this.gson = new Gson();
    }

    public DiscordTokenResponse exchangeCode(String code) throws IOException, InterruptedException {
        String form = formField("client_id", config.discordClientId())
            + "&" + formField("client_secret", config.discordClientSecret())
            + "&" + formField("grant_type", "authorization_code")
            + "&" + formField("code", code)
            + "&" + formField("redirect_uri", config.discordRedirectUri());

        HttpRequest request = HttpRequest.newBuilder(TOKEN_URI)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(form))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Discord token exchange failed with status " + response.statusCode());
        }

        JsonObject json = gson.fromJson(response.body(), JsonObject.class);
        return new DiscordTokenResponse(
            json.get("access_token").getAsString(),
            json.get("token_type").getAsString(),
            json.get("expires_in").getAsInt(),
            json.has("scope") ? json.get("scope").getAsString() : ""
        );
    }

    private String formField(String key, String value) {
        return encode(key) + "=" + encode(value);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
