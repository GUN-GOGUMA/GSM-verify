package dev.gungoguma.verify.server;

import dev.gungoguma.verify.bungee.BungeeConnector;
import dev.gungoguma.verify.discord.DiscordAnnouncementClient;
import dev.gungoguma.verify.discord.DiscordApiException;
import dev.gungoguma.verify.discord.DiscordOAuthClient;
import dev.gungoguma.verify.discord.DiscordTokenResponse;
import dev.gungoguma.verify.discord.DiscordVerificationResult;
import dev.gungoguma.verify.discord.DiscordVerificationService;
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
            return OAuthCallbackResult.failure("Discord 인증이 취소되었거나 실패했습니다.");
        }

        String code = query.get("code");
        String state = query.get("state");
        if (isBlank(code) || isBlank(state)) {
            return OAuthCallbackResult.failure("인증 요청에 필요한 값이 없습니다. 게임에서 /verify를 다시 실행해 주세요.");
        }

        Optional<PendingVerification> pending = pendingStore.findByState(state);
        if (pending.isEmpty()) {
            return OAuthCallbackResult.failure("인증 요청이 만료되었습니다. 게임에서 /verify를 다시 실행해 주세요.");
        }

        PendingVerification verification = pending.get();
        if (verificationStore.isVerified(verification.uuid())) {
            pendingStore.remove(verification);
            return OAuthCallbackResult.failure("이미 인증된 Minecraft 계정입니다.");
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
                return OAuthCallbackResult.failure("이미 다른 Minecraft 계정에 연결된 Discord 계정입니다.");
            }

            verificationStore.save(verifiedUser);
            pendingStore.remove(verification);
            announcementClient.sendVerificationSuccess(verifiedUser);
            connectOnlinePlayer(verification);
            return OAuthCallbackResult.success("인증이 완료되었습니다. 게임으로 돌아가 주세요.");
        } catch (DiscordApiException exception) {
            logger.log(Level.WARNING, exception.getMessage());
            return OAuthCallbackResult.failure(discordApiFailureMessage(exception));
        } catch (IOException exception) {
            logger.log(Level.WARNING, "Discord verification failed: " + exception.getMessage());
            return OAuthCallbackResult.failure("Discord 인증 처리 중 네트워크 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return OAuthCallbackResult.failure("Discord 인증 처리가 중단되었습니다. 잠시 후 다시 시도해 주세요.");
        } catch (Exception exception) {
            logger.log(Level.WARNING, "Discord verification failed.", exception);
            return OAuthCallbackResult.failure("Discord 인증 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");
        }
    }

    private String discordApiFailureMessage(DiscordApiException exception) {
        if (exception.isUnauthorized()) {
            return "Discord 봇 권한 또는 토큰 설정을 확인해 주세요.";
        }

        if (exception.isRateLimited()) {
            return "Discord 요청이 잠시 제한되었습니다. 잠시 후 다시 시도해 주세요.";
        }

        return "Discord 인증 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.";
    }

    private void connectOnlinePlayer(PendingVerification verification) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            Player player = Bukkit.getPlayer(verification.uuid());
            if (player != null && player.isOnline()) {
                bungeeConnector.connectToSmp(player);
            }
        });
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
