package dev.gungoguma.verify.discord;

import dev.gungoguma.verify.config.VerifyConfig;
import dev.gungoguma.verify.model.RoleType;
import dev.gungoguma.verify.model.VerifiedUser;
import java.time.Instant;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DiscordVerificationService {
    private static final Pattern GRADUATE_NICKNAME = Pattern.compile("^(\\d+)기\\s+(.+)$");
    private static final Pattern STUDENT_NICKNAME = Pattern.compile("^(\\d{4})\\s+(.+)$");

    private final VerifyConfig config;
    private final DiscordClient apiClient;

    public DiscordVerificationService(VerifyConfig config, DiscordClient apiClient) {
        this.config = config;
        this.apiClient = apiClient;
    }

    public DiscordVerificationResult verify(UUID uuid, DiscordTokenResponse token) throws Exception {
        DiscordUser user = apiClient.fetchCurrentUser(token.accessToken());
        DiscordGuildMember member = apiClient.fetchGuildMember(user.id());
        if (member == null) {
            return DiscordVerificationResult.failure("GSM Discord 서버에 참가한 뒤 다시 인증해 주세요.");
        }

        String nickname = member.nick();
        if (nickname == null || nickname.isBlank()) {
            return DiscordVerificationResult.failure("Discord 서버 프로필 닉네임을 설정한 뒤 다시 인증해 주세요.");
        }

        if (member.roles().contains(config.graduateRoleId())) {
            return verifyGraduate(uuid, user.id(), nickname);
        }

        if (member.roles().contains(config.studentRoleId())) {
            return verifyStudent(uuid, user.id(), nickname);
        }

        return DiscordVerificationResult.failure("인증에 필요한 Discord 역할이 없습니다.");
    }

    private DiscordVerificationResult verifyGraduate(UUID uuid, String discordId, String nickname) {
        Matcher matcher = GRADUATE_NICKNAME.matcher(nickname);
        if (!matcher.matches()) {
            return DiscordVerificationResult.failure("닉네임 형식이 올바르지 않습니다. 예: 1기 홍길동");
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
            return DiscordVerificationResult.failure("닉네임 형식이 올바르지 않습니다. 예: 1234 홍길동");
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
