package dev.gungoguma.verify.discord;

import dev.gungoguma.verify.config.VerifyConfig;
import dev.gungoguma.verify.model.RoleType;
import dev.gungoguma.verify.model.VerifiedUser;
import java.time.Instant;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DiscordVerificationService {
    private static final Pattern GRADUATE_NICKNAME = Pattern.compile("^(\\d+)\\uAE30\\s+(.+)$");
    private static final Pattern STUDENT_NICKNAME = Pattern.compile("^(\\d{4})\\s+(.+)$");

    private final VerifyConfig config;
    private final DiscordClient apiClient;

    public DiscordVerificationService(VerifyConfig config, DiscordClient apiClient) {
        this.config = config;
        this.apiClient = apiClient;
    }

    public DiscordVerificationResult verify(UUID uuid, DiscordTokenResponse token) throws Exception {
        DiscordUser user = apiClient.fetchCurrentUser(token.accessToken());
        boolean joinedGsmGuild = apiClient.fetchCurrentUserGuilds(token.accessToken()).stream()
            .anyMatch(guild -> config.guildId().equals(guild.id()));
        if (!joinedGsmGuild) {
            return DiscordVerificationResult.failure("GSM Discord server membership is required.");
        }

        DiscordGuildMember member = apiClient.fetchCurrentUserGuildMember(token.accessToken());
        if (member == null) {
            return DiscordVerificationResult.failure("GSM Discord server membership is required.");
        }

        String nickname = member.nick();
        if (nickname == null || nickname.isBlank()) {
            return DiscordVerificationResult.failure("Set your Discord server profile nickname and try again.");
        }

        if (member.roles().contains(config.graduateRoleId())) {
            return verifyGraduate(uuid, user.id(), nickname);
        }

        if (member.roles().contains(config.studentRoleId())) {
            return verifyStudent(uuid, user.id(), nickname);
        }

        return DiscordVerificationResult.failure("Required Discord role was not found.");
    }

    private DiscordVerificationResult verifyGraduate(UUID uuid, String discordId, String nickname) {
        Matcher matcher = GRADUATE_NICKNAME.matcher(nickname);
        if (!matcher.matches()) {
            return DiscordVerificationResult.failure("Invalid nickname format. Example: 1기 John");
        }

        return DiscordVerificationResult.success(
            new VerifiedUser(
                uuid,
                discordId,
                matcher.group(2).trim(),
                Integer.parseInt(matcher.group(1)),
                null,
                RoleType.GRADUATE,
                Instant.now()
            )
        );
    }

    private DiscordVerificationResult verifyStudent(UUID uuid, String discordId, String nickname) {
        Matcher matcher = STUDENT_NICKNAME.matcher(nickname);
        if (!matcher.matches()) {
            return DiscordVerificationResult.failure("Invalid nickname format. Example: 1234 John");
        }

        return DiscordVerificationResult.success(
            new VerifiedUser(
                uuid,
                discordId,
                matcher.group(2).trim(),
                null,
                matcher.group(1),
                RoleType.STUDENT,
                Instant.now()
            )
        );
    }
}
