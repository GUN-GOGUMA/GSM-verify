package dev.gungoguma.verify;

import dev.gungoguma.verify.bungee.BungeeConnector;
import dev.gungoguma.verify.config.VerifyConfig;
import dev.gungoguma.verify.listener.PlayerJoinListener;
import dev.gungoguma.verify.verification.NoopVerificationLookup;
import dev.gungoguma.verify.verification.VerificationLookup;
import org.bukkit.plugin.java.JavaPlugin;

public final class GsmVerify extends JavaPlugin {
    private VerifyConfig verifyConfig;
    private BungeeConnector bungeeConnector;
    private VerificationLookup verificationLookup;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        verifyConfig = VerifyConfig.load(getConfig());
        bungeeConnector = new BungeeConnector(this, verifyConfig);
        verificationLookup = new NoopVerificationLookup();

        if (verifyConfig.hasMissingRequiredKeys()) {
            getLogger().warning(verifyConfig.configMissingMessage());
            getLogger().warning("Missing config keys: " + String.join(", ", verifyConfig.missingRequiredKeys()));
        }

        bungeeConnector.registerChannel();
        getServer().getPluginManager().registerEvents(
            new PlayerJoinListener(this, verificationLookup, bungeeConnector),
            this
        );

        getLogger().info("GSM-verify enabled.");
    }

    @Override
    public void onDisable() {
        if (bungeeConnector != null) {
            bungeeConnector.unregisterChannel();
        }

        getLogger().info("GSM-verify disabled.");
    }

    public VerifyConfig verifyConfig() {
        return verifyConfig;
    }

    public BungeeConnector bungeeConnector() {
        return bungeeConnector;
    }
}
