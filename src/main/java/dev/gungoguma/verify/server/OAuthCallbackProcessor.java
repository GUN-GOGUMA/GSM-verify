package dev.gungoguma.verify.server;

import dev.gungoguma.verify.bungee.BungeeConnector;
import dev.gungoguma.verify.discord.DiscordAnnouncementClient;
import dev.gungoguma.verify.discord.DiscordApiException;
import dev.gungoguma.verify.discord.DiscordOAuthClient;
import dev.gungoguma.verify.discord.DiscordTokenResponse;
import dev.gungoguma.verify.discord.DiscordVerificationResult;
import dev.gungoguma.verify.discord.DiscordVerificationService;
import dev.gungoguma.verify.event.PlayerVerifiedEvent;
import dev.gungoguma.verify.model.VerifiedUser;
import dev.gungoguma.verify.oauth.PendingVerification;
import dev.gungoguma.verify.oauth.PendingVerificationStore;
import dev.gungoguma.verify.storage.VerificationStore;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class OAuthCallbackProcessor {
    private final JavaPlugin plugin;
    private final PendingVerificationStore pendingStore;
    private final VerificationStore verificationStore;
    private final DiscordOAuthClient oauthClient;
    private final DiscordVerificationService verificationService;
    private final DiscordAnnouncementClient announcementClient;
    private final BungeeConnector bungeeConnector;
    private final Logger logger;

    public OAuthCallbackProcessor(
        JavaPlugin plugin,
        PendingVerificationStore pendingStore,
        VerificationStore verificationStore,
        DiscordOAuthClient oauthClient,
        DiscordVerificationService verificationService,
        DiscordAnnouncementClient announcementClient,
        BungeeConnector bungeeConnector,
        Logger logger
    ) {
        this.plugin = plugin;
        this.pendingStore = pendingStore;
        this.verificationStore = verificationStore;
        this.oauthClient = oauthClient;
        this.verificationService = verificationService;
        this.announcementClient = announcementClient;
        this.bungeeConnector = bungeeConnector;
        this.logger = logger;
    }

    public OAuthCallbackResult process(Map<String, String> query) {
        if (query.containsKey("error")) {
            return OAuthCallbackResult.failure("Discord verification was cancelled or failed.");
        }

        String code = query.get("code");
        String state = query.get("state");
        if (isBlank(code) || isBlank(state)) {
            return OAuthCallbackResult.failure("Missing verification request values. Run /verify again in game.");
        }

        Optional<PendingVerification> pending = pendingStore.findByState(state);
        if (pending.isEmpty()) {
            return OAuthCallbackResult.failure("The verification request expired. Run /verify again in game.");
        }

        PendingVerification verification = pending.get();
        if (verificationStore.isVerified(verification.uuid())) {
            pendingStore.remove(verification);
            return OAuthCallbackResult.failure("This Minecraft account is already verified.");
        }

        try {
            DiscordTokenResponse token = oauthClient.exchangeCode(code);
            DiscordVerificationResult result = verificationService.verify(verification.uuid(), token);
            if (!result.success()) {
                return OAuthCallbackResult.failure(result.failureMessage());
            }

            VerifiedUser verifiedUser = result.user();
            if (verificationStore.findByDiscordId(verifiedUser.discordId()).isPresent()) {
                pendingStore.remove(verification);
                return OAuthCallbackResult.failure("This Discord account is already linked to another Minecraft account.");
            }

            verificationStore.save(verifiedUser);
            pendingStore.remove(verification);
            announcementClient.sendVerificationSuccess(verifiedUser);
            publishVerifiedEventAndConnect(verification, verifiedUser);
            return OAuthCallbackResult.success("Verification complete. Return to the game.");
        } catch (DiscordApiException exception) {
            logger.log(Level.WARNING, exception.getMessage());
            return OAuthCallbackResult.failure(discordApiFailureMessage(exception));
        } catch (IOException exception) {
            logger.log(Level.WARNING, "Discord verification failed: " + exception.getMessage());
            return OAuthCallbackResult.failure("A network error occurred while processing Discord verification. Try again later.");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return OAuthCallbackResult.failure("Discord verification was interrupted. Try again later.");
        } catch (Exception exception) {
            logger.log(Level.WARNING, "Discord verification failed.", exception);
            return OAuthCallbackResult.failure("An error occurred while processing Discord verification. Try again later.");
        }
    }

    private String discordApiFailureMessage(DiscordApiException exception) {
        if (exception.isUnauthorized()) {
            return "Check the Discord bot permissions or token setting.";
        }

        if (exception.isRateLimited()) {
            return "Discord requests are temporarily rate limited. Try again later.";
        }

        return "An error occurred while processing Discord verification. Try again later.";
    }

    private void publishVerifiedEventAndConnect(PendingVerification verification, VerifiedUser verifiedUser) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            Player player = Bukkit.getPlayer(verification.uuid());
            if (player == null || !player.isOnline()) {
                return;
            }

            Bukkit.getPluginManager().callEvent(new PlayerVerifiedEvent(player, verifiedUser));
            bungeeConnector.connectToSmp(player);
        });
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
