package dev.gungoguma.verify;

import dev.gungoguma.verify.bungee.BungeeConnector;
import dev.gungoguma.verify.command.VerifyCommand;
import dev.gungoguma.verify.config.VerifyConfig;
import dev.gungoguma.verify.discord.DiscordApiClient;
import dev.gungoguma.verify.discord.DiscordAnnouncementClient;
import dev.gungoguma.verify.discord.DiscordOAuthClient;
import dev.gungoguma.verify.discord.DiscordVerificationService;
import dev.gungoguma.verify.listener.PlayerJoinListener;
import dev.gungoguma.verify.listener.QueueIsolationListener;
import dev.gungoguma.verify.oauth.DiscordOAuthUrlBuilder;
import dev.gungoguma.verify.oauth.PendingVerificationStore;
import dev.gungoguma.verify.server.OAuthCallbackProcessor;
import dev.gungoguma.verify.server.OAuthHttpServer;
import dev.gungoguma.verify.storage.AnnualVerificationReset;
import dev.gungoguma.verify.storage.VerificationStore;
import dev.gungoguma.verify.storage.YamlVerificationStore;
import java.io.IOException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;
import org.bukkit.plugin.java.JavaPlugin;

public final class GsmVerify extends JavaPlugin {
    private VerifyConfig verifyConfig;
    private BungeeConnector bungeeConnector;
    private VerificationStore verificationStore;
    private AnnualVerificationReset annualReset;
    private PendingVerificationStore pendingVerificationStore;
    private OAuthHttpServer oauthHttpServer;
    private DiscordAnnouncementClient announcementClient;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        verifyConfig = VerifyConfig.load(getConfig());
        bungeeConnector = new BungeeConnector(this, verifyConfig);
        pendingVerificationStore = new PendingVerificationStore(Clock.systemUTC(), verifyConfig.stateExpireSeconds());
        announcementClient = new DiscordAnnouncementClient(verifyConfig, getLogger());

        if (!validateConfig()) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        try {
            verificationStore = new YamlVerificationStore(getDataFolder());
            annualReset = new AnnualVerificationReset(
                getDataFolder(),
                verifyConfig,
                verificationStore
            );
            if (annualReset.resetIfNeeded(LocalDate.now())) {
                getLogger().info("Verification data was reset for this year.");
                announcementClient.sendAnnualReset(LocalDate.now());
            }
        } catch (IOException exception) {
            getLogger().severe("Failed to initialize verification storage: " + exception.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        try {
            startOAuthHttpServer();
        } catch (IOException exception) {
            getLogger().severe("Failed to start OAuth callback server: " + exception.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        startAnnualResetTask();

        bungeeConnector.registerChannel();
        registerCommands();
        getServer().getPluginManager().registerEvents(
            new PlayerJoinListener(this, this::verifyConfig, verificationStore, pendingVerificationStore, bungeeConnector),
            this
        );
        getServer().getPluginManager().registerEvents(
            new QueueIsolationListener(this, this::verifyConfig),
            this
        );
        startPendingCleanupTask();

        getLogger().info("GSM-verify enabled.");
    }

    @Override
    public void onDisable() {
        if (oauthHttpServer != null) {
            oauthHttpServer.stop();
        }

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

    public PendingVerificationStore pendingVerificationStore() {
        return pendingVerificationStore;
    }

    public OAuthHttpServer oauthHttpServer() {
        return oauthHttpServer;
    }

    public boolean reloadVerifyConfig() {
        VerifyConfig previousConfig = verifyConfig;
        reloadConfig();
        verifyConfig = VerifyConfig.load(getConfig());
        if (!validateConfig()) {
            verifyConfig = previousConfig;
            return false;
        }

        bungeeConnector.updateConfig(verifyConfig);
        announcementClient = new DiscordAnnouncementClient(verifyConfig, getLogger());
        annualReset = new AnnualVerificationReset(getDataFolder(), verifyConfig, verificationStore);

        try {
            restartOAuthHttpServer();
        } catch (IOException exception) {
            getLogger().severe("Failed to restart OAuth callback server: " + exception.getMessage());
            verifyConfig = previousConfig;
            bungeeConnector.updateConfig(previousConfig);
            announcementClient = new DiscordAnnouncementClient(previousConfig, getLogger());
            annualReset = new AnnualVerificationReset(getDataFolder(), previousConfig, verificationStore);
            try {
                restartOAuthHttpServer();
            } catch (IOException rollbackException) {
                getLogger().severe("Failed to restore OAuth callback server: " + rollbackException.getMessage());
            }
            return false;
        }

        return true;
    }

    public boolean validateConfig() {
        boolean valid = true;
        if (verifyConfig.hasMissingRequiredKeys()) {
            getLogger().warning(verifyConfig.configMissingMessage());
            getLogger().warning("Missing config keys: " + String.join(", ", verifyConfig.missingRequiredKeys()));
            valid = false;
        }
        if (verifyConfig.hasInvalidKeys()) {
            getLogger().warning("Invalid config keys: " + String.join(", ", verifyConfig.invalidKeys()));
            valid = false;
        }
        return valid;
    }

    private void registerCommands() {
        VerifyCommand verifyCommand = new VerifyCommand(
            this,
            verifyConfig,
            verificationStore,
            pendingVerificationStore,
            new DiscordOAuthUrlBuilder(verifyConfig)
        );
        Objects.requireNonNull(getCommand("verify"), "verify command is not registered").setExecutor(verifyCommand);
    }

    private void startAnnualResetTask() {
        long oneDayTicks = 20L * 60L * 60L * 24L;
        getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            try {
                if (annualReset.resetIfNeeded(LocalDate.now())) {
                    getLogger().info("Verification data was reset for this year.");
                    announcementClient.sendAnnualReset(LocalDate.now());
                }
            } catch (IOException exception) {
                getLogger().severe("Failed to reset verification data: " + exception.getMessage());
            }
        }, oneDayTicks, oneDayTicks);
    }

    private void startPendingCleanupTask() {
        getServer().getScheduler().runTaskTimerAsynchronously(
            this,
            pendingVerificationStore::cleanupExpired,
            20L * 60L,
            20L * 60L
        );
    }

    private void restartOAuthHttpServer() throws IOException {
        if (oauthHttpServer != null) {
            oauthHttpServer.stop();
            oauthHttpServer = null;
        }
        startOAuthHttpServer();
    }

    private void startOAuthHttpServer() throws IOException {
        OAuthCallbackProcessor callbackProcessor = new OAuthCallbackProcessor(
            this,
            pendingVerificationStore,
            verificationStore,
            new DiscordOAuthClient(verifyConfig),
            new DiscordVerificationService(verifyConfig, new DiscordApiClient(verifyConfig)),
            announcementClient,
            bungeeConnector,
            getLogger()
        );
        oauthHttpServer = new OAuthHttpServer(verifyConfig, callbackProcessor);
        oauthHttpServer.start();
        getLogger().info(
            "OAuth callback server started on "
                + verifyConfig.oauthServerHost()
                + ":"
                + verifyConfig.oauthServerPort()
                + verifyConfig.oauthCallbackPath()
        );
    }
}
