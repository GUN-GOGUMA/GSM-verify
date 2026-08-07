package dev.gungoguma.verify;

import dev.gungoguma.verify.config.VerifyConfig;
import org.bukkit.configuration.file.YamlConfiguration;

public final class TestConfigs {
    private TestConfigs() {
    }

    public static VerifyConfig verifyConfig() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("discord.clientId", "client");
        config.set("discord.clientSecret", "secret");
        config.set("discord.redirectUri", "http://localhost:27073/callback");
        config.set("discord.guildId", "guild");
        config.set("discord.graduateRoleId", "graduate");
        config.set("discord.studentRoleId", "student");
        config.set("oauthServer.host", "127.0.0.1");
        config.set("oauthServer.port", 27073);
        config.set("oauthServer.callbackPath", "/callback");
        config.set("server.smpName", "smp");
        config.set("verification.stateExpireSeconds", 300);
        config.set("verification.resetMonth", 1);
        config.set("verification.resetDay", 12);
        config.set("messages.prefix", "[GSM-Verify]");
        return VerifyConfig.load(config);
    }
}
