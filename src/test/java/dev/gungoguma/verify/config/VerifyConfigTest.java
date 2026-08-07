package dev.gungoguma.verify.config;

import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.gungoguma.verify.TestConfigs;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

final class VerifyConfigTest {
    @Test
    void rejectsImpossibleResetDate() {
        YamlConfiguration config = new YamlConfiguration();
        VerifyConfig valid = TestConfigs.verifyConfig();
        config.set("discord.clientId", valid.discordClientId());
        config.set("discord.clientSecret", valid.discordClientSecret());
        config.set("discord.redirectUri", valid.discordRedirectUri());
        config.set("discord.guildId", valid.guildId());
        config.set("discord.graduateRoleId", valid.graduateRoleId());
        config.set("discord.studentRoleId", valid.studentRoleId());
        config.set("oauthServer.host", valid.oauthServerHost());
        config.set("oauthServer.port", valid.oauthServerPort());
        config.set("oauthServer.callbackPath", valid.oauthCallbackPath());
        config.set("server.smpName", valid.smpServerName());
        config.set("verification.stateExpireSeconds", valid.stateExpireSeconds());
        config.set("verification.resetMonth", 2);
        config.set("verification.resetDay", 31);

        VerifyConfig invalid = VerifyConfig.load(config);

        assertTrue(invalid.invalidKeys().contains("verification.resetDay"));
    }
}
