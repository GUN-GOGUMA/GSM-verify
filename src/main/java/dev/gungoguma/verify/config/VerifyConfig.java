package dev.gungoguma.verify.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.configuration.file.FileConfiguration;

public final class VerifyConfig {
    private final String discordClientId;
    private final String discordClientSecret;
    private final String discordRedirectUri;
    private final String discordBotToken;
    private final String guildId;
    private final String graduateRoleId;
    private final String studentRoleId;
    private final String httpHost;
    private final int httpPort;
    private final String httpCallbackPath;
    private final String smpServerName;
    private final int stateExpireSeconds;
    private final int resetMonth;
    private final int resetDay;
    private final String messagePrefix;
    private final String configMissingMessage;
    private final List<String> missingRequiredKeys;

    private VerifyConfig(FileConfiguration config) {
        this.discordClientId = config.getString("discord.clientId", "");
        this.discordClientSecret = config.getString("discord.clientSecret", "");
        this.discordRedirectUri = config.getString("discord.redirectUri", "");
        this.discordBotToken = config.getString("discord.botToken", "");
        this.guildId = config.getString("discord.guildId", "");
        this.graduateRoleId = config.getString("discord.graduateRoleId", "");
        this.studentRoleId = config.getString("discord.studentRoleId", "");
        this.httpHost = config.getString("http.host", "0.0.0.0");
        this.httpPort = config.getInt("http.port", 27073);
        this.httpCallbackPath = normalizePath(config.getString("http.callbackPath", "/callback"));
        this.smpServerName = config.getString("server.smpName", "smp");
        this.stateExpireSeconds = config.getInt("verification.stateExpireSeconds", 300);
        this.resetMonth = config.getInt("verification.resetMonth", 1);
        this.resetDay = config.getInt("verification.resetDay", 12);
        this.messagePrefix = config.getString("messages.prefix", "[GSM-Verify]");
        this.configMissingMessage = config.getString(
            "messages.configMissing",
            "GSM-Verify 설정에서 필수 항목이 비어 있습니다. config.yml을 확인해 주세요."
        );
        this.missingRequiredKeys = findMissingRequiredKeys();
    }

    public static VerifyConfig load(FileConfiguration config) {
        return new VerifyConfig(config);
    }

    public String discordClientId() {
        return discordClientId;
    }

    public String discordClientSecret() {
        return discordClientSecret;
    }

    public String discordRedirectUri() {
        return discordRedirectUri;
    }

    public String discordBotToken() {
        return discordBotToken;
    }

    public String guildId() {
        return guildId;
    }

    public String graduateRoleId() {
        return graduateRoleId;
    }

    public String studentRoleId() {
        return studentRoleId;
    }

    public String httpHost() {
        return httpHost;
    }

    public int httpPort() {
        return httpPort;
    }

    public String httpCallbackPath() {
        return httpCallbackPath;
    }

    public String smpServerName() {
        return smpServerName;
    }

    public int stateExpireSeconds() {
        return stateExpireSeconds;
    }

    public int resetMonth() {
        return resetMonth;
    }

    public int resetDay() {
        return resetDay;
    }

    public String messagePrefix() {
        return messagePrefix;
    }

    public String configMissingMessage() {
        return configMissingMessage;
    }

    public boolean hasMissingRequiredKeys() {
        return !missingRequiredKeys.isEmpty();
    }

    public List<String> missingRequiredKeys() {
        return Collections.unmodifiableList(missingRequiredKeys);
    }

    private List<String> findMissingRequiredKeys() {
        List<String> missing = new ArrayList<>();
        requireNonBlank(missing, "discord.clientId", discordClientId);
        requireNonBlank(missing, "discord.clientSecret", discordClientSecret);
        requireNonBlank(missing, "discord.redirectUri", discordRedirectUri);
        requireNonBlank(missing, "discord.botToken", discordBotToken);
        requireNonBlank(missing, "discord.guildId", guildId);
        requireNonBlank(missing, "discord.graduateRoleId", graduateRoleId);
        requireNonBlank(missing, "discord.studentRoleId", studentRoleId);
        requireNonBlank(missing, "http.host", httpHost);
        requireNonBlank(missing, "http.callbackPath", httpCallbackPath);
        requireNonBlank(missing, "server.smpName", smpServerName);
        return missing;
    }

    private static void requireNonBlank(List<String> missing, String key, String value) {
        if (value == null || value.isBlank()) {
            missing.add(key);
        }
    }

    private static String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "/callback";
        }

        return path.startsWith("/") ? path : "/" + path;
    }
}
