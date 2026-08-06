package dev.gungoguma.verify.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.time.YearMonth;
import org.bukkit.configuration.file.FileConfiguration;

public final class VerifyConfig {
    private final String discordClientId;
    private final String discordClientSecret;
    private final String discordRedirectUri;
    private final String discordBotToken;
    private final String guildId;
    private final String graduateRoleId;
    private final String studentRoleId;
    private final String announcementChannelId;
    private final String oauthServerHost;
    private final int oauthServerPort;
    private final String oauthCallbackPath;
    private final String smpServerName;
    private final int stateExpireSeconds;
    private final int resetMonth;
    private final int resetDay;
    private final String messagePrefix;
    private final String configMissingMessage;
    private final List<String> missingRequiredKeys;
    private final List<String> invalidKeys;

    private VerifyConfig(FileConfiguration config) {
        this.discordClientId = config.getString("discord.clientId", "");
        this.discordClientSecret = config.getString("discord.clientSecret", "");
        this.discordRedirectUri = config.getString("discord.redirectUri", "");
        this.discordBotToken = config.getString("discord.botToken", "");
        this.guildId = config.getString("discord.guildId", "");
        this.graduateRoleId = config.getString("discord.graduateRoleId", "");
        this.studentRoleId = config.getString("discord.studentRoleId", "");
        this.announcementChannelId = config.getString("discord.announcementChannelId", "");
        this.oauthServerHost = config.getString("oauthServer.host", "0.0.0.0");
        this.oauthServerPort = config.getInt("oauthServer.port", 27073);
        this.oauthCallbackPath = normalizePath(config.getString("oauthServer.callbackPath", "/callback"));
        this.smpServerName = config.getString("server.smpName", "smp");
        this.stateExpireSeconds = config.getInt("verification.stateExpireSeconds", 300);
        this.resetMonth = config.getInt("verification.resetMonth", 1);
        this.resetDay = config.getInt("verification.resetDay", 12);
        this.messagePrefix = config.getString("messages.prefix", "[GSM-Verify]");
        this.configMissingMessage = config.getString(
            "messages.configMissing",
            "Required GSM-Verify config values are missing. Please check config.yml."
        );
        this.missingRequiredKeys = findMissingRequiredKeys();
        this.invalidKeys = findInvalidKeys();
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

    public String announcementChannelId() {
        return announcementChannelId;
    }

    public String oauthServerHost() {
        return oauthServerHost;
    }

    public int oauthServerPort() {
        return oauthServerPort;
    }

    public String oauthCallbackPath() {
        return oauthCallbackPath;
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

    public boolean hasInvalidKeys() {
        return !invalidKeys.isEmpty();
    }

    public List<String> invalidKeys() {
        return Collections.unmodifiableList(invalidKeys);
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
        requireNonBlank(missing, "discord.announcementChannelId", announcementChannelId);
        requireNonBlank(missing, "oauthServer.host", oauthServerHost);
        requireNonBlank(missing, "oauthServer.callbackPath", oauthCallbackPath);
        requireNonBlank(missing, "server.smpName", smpServerName);
        return missing;
    }

    private List<String> findInvalidKeys() {
        List<String> invalid = new ArrayList<>();
        if (oauthServerPort < 1 || oauthServerPort > 65535) {
            invalid.add("oauthServer.port");
        }
        if (stateExpireSeconds < 60) {
            invalid.add("verification.stateExpireSeconds");
        }
        if (resetMonth < 1 || resetMonth > 12) {
            invalid.add("verification.resetMonth");
        }
        if (resetDay < 1 || resetDay > 31 || !isValidResetDate()) {
            invalid.add("verification.resetDay");
        }
        if (!oauthCallbackPath.startsWith("/")) {
            invalid.add("oauthServer.callbackPath");
        }
        return invalid;
    }

    private boolean isValidResetDate() {
        if (resetMonth < 1 || resetMonth > 12 || resetDay < 1) {
            return false;
        }

        return YearMonth.of(2000, resetMonth).isValidDay(resetDay);
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
