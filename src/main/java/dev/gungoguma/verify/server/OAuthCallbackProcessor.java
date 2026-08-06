package dev.gungoguma.verify.server;

import dev.gungoguma.verify.discord.DiscordOAuthClient;
import dev.gungoguma.verify.oauth.PendingVerification;
import dev.gungoguma.verify.oauth.PendingVerificationStore;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class OAuthCallbackProcessor {
    private final PendingVerificationStore pendingStore;
    private final DiscordOAuthClient oauthClient;
    private final Logger logger;

    public OAuthCallbackProcessor(
        PendingVerificationStore pendingStore,
        DiscordOAuthClient oauthClient,
        Logger logger
    ) {
        this.pendingStore = pendingStore;
        this.oauthClient = oauthClient;
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

        try {
            oauthClient.exchangeCode(code);
            PendingVerification verification = pending.get();
            return OAuthCallbackResult.success(
                verification.playerName() + "님의 Discord 인증 응답을 확인했습니다. 게임으로 돌아가 주세요."
            );
        } catch (IOException exception) {
            logger.log(Level.WARNING, "Discord OAuth token exchange failed: " + exception.getMessage());
            return OAuthCallbackResult.failure("Discord 인증 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return OAuthCallbackResult.failure("Discord 인증 처리가 중단되었습니다. 잠시 후 다시 시도해 주세요.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
