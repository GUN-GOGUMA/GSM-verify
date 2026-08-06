package dev.gungoguma.verify;

import dev.gungoguma.verify.bungee.BungeeConnector;
import dev.gungoguma.verify.config.VerifyConfig;
import dev.gungoguma.verify.listener.PlayerJoinListener;
import dev.gungoguma.verify.storage.AnnualVerificationReset;
import dev.gungoguma.verify.storage.VerificationStore;
import dev.gungoguma.verify.storage.YamlVerificationStore;
import java.io.IOException;
import java.time.LocalDate;
import org.bukkit.plugin.java.JavaPlugin;

public final class GsmVerify extends JavaPlugin {
    private VerifyConfig verifyConfig;
    private BungeeConnector bungeeConnector;
    private VerificationStore verificationStore;
    private AnnualVerificationReset annualReset;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        verifyConfig = VerifyConfig.load(getConfig());
        bungeeConnector = new BungeeConnector(this, verifyConfig);

        try {
            verificationStore = new YamlVerificationStore(getDataFolder());
            annualReset = new AnnualVerificationReset(
                getDataFolder(),
                verifyConfig,
                verificationStore
            );
            if (annualReset.resetIfNeeded(LocalDate.now())) {
                getLogger().info("Verification data was reset for this year.");
            }
        } catch (IOException exception) {
            getLogger().severe("Failed to initialize verification storage: " + exception.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        startAnnualResetTask();

        if (verifyConfig.hasMissingRequiredKeys()) {
            getLogger().warning(verifyConfig.configMissingMessage());
            getLogger().warning("Missing config keys: " + String.join(", ", verifyConfig.missingRequiredKeys()));
        }

        bungeeConnector.registerChannel();
        getServer().getPluginManager().registerEvents(
            new PlayerJoinListener(this, verificationStore, bungeeConnector),
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

    public VerificationStore verificationStore() {
        return verificationStore;
    }

    private void startAnnualResetTask() {
        long oneDayTicks = 20L * 60L * 60L * 24L;
        getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            try {
                if (annualReset.resetIfNeeded(LocalDate.now())) {
                    getLogger().info("Verification data was reset for this year.");
                }
            } catch (IOException exception) {
                getLogger().severe("Failed to reset verification data: " + exception.getMessage());
            }
        }, oneDayTicks, oneDayTicks);
    }
}
