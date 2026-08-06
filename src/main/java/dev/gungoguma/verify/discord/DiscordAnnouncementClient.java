package dev.gungoguma.verify.discord;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.gungoguma.verify.config.VerifyConfig;
import dev.gungoguma.verify.model.VerifiedUser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class DiscordAnnouncementClient {
    private static final int GREEN = 0x22C55E;
    private static final int BLUE = 0x3B82F6;

    private final VerifyConfig config;
    private final Logger logger;
    private final HttpClient httpClient;

    public DiscordAnnouncementClient(VerifyConfig config, Logger logger) {
        this.config = config;
        this.logger = logger;
        this.httpClient = HttpClient.newHttpClient();
    }

    public CompletableFuture<Void> sendVerificationSuccess(VerifiedUser user) {
        String identity = user.flag() != null ? user.flag() + "기" : user.studentId();
        String description = identity + " " + user.name() + "님이 인증하였습니다.";
        return sendEmbed("GSM 인증 완료", description, GREEN);
    }

    public CompletableFuture<Void> sendAnnualReset(LocalDate resetDate) {
        return sendEmbed(
            "GSM 인증 정보 초기화",
            resetDate + " 기준으로 Queue 로컬 인증 정보가 초기화되었습니다.",
            BLUE
        );
    }

    private CompletableFuture<Void> sendEmbed(String title, String description, int color) {
        if (config.announcementChannelId().isBlank()) {
            return CompletableFuture.completedFuture(null);
        }

        JsonObject embed = new JsonObject();
        embed.addProperty("title", title);
        embed.addProperty("description", description);
        embed.addProperty("color", color);

        JsonArray embeds = new JsonArray();
        embeds.add(embed);

        JsonObject payload = new JsonObject();
        payload.add("embeds", embeds);

        HttpRequest request = HttpRequest.newBuilder(channelMessageUri())
            .header("Authorization", "Bot " + config.discordBotToken())
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
            .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding())
            .thenAccept(response -> {
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    logger.warning("Discord announcement failed with status " + response.statusCode());
                }
            })
            .exceptionally(exception -> {
                logger.log(Level.WARNING, "Discord announcement failed: " + exception.getMessage());
                return null;
            });
    }

    private URI channelMessageUri() {
        return URI.create(
            "https://discord.com/api/v10/channels/" + config.announcementChannelId() + "/messages"
        );
    }
}
