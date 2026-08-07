package dev.gungoguma.verify.oauth;

import dev.gungoguma.verify.config.VerifyConfig;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public final class DiscordOAuthUrlBuilder {
    private static final String AUTHORIZE_URL = "https://discord.com/oauth2/authorize";

    private final VerifyConfig config;

    public DiscordOAuthUrlBuilder(VerifyConfig config) {
        this.config = config;
    }

    public String build(String state) {
        return AUTHORIZE_URL
            + "?response_type=code"
            + "&client_id=" + encode(config.discordClientId())
            + "&redirect_uri=" + encode(config.discordRedirectUri())
            + "&scope=" + encode("identify guilds guilds.members.read")
            + "&state=" + encode(state);
    }

    private String encode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException exception) {
            throw new IllegalStateException("UTF-8 encoding is not available.", exception);
        }
    }
}
