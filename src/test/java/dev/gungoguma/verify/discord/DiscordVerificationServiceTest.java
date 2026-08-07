package dev.gungoguma.verify.discord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.gungoguma.verify.TestConfigs;
import dev.gungoguma.verify.model.RoleType;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class DiscordVerificationServiceTest {
    @Test
    void verifiesGraduateNickname() throws Exception {
        DiscordVerificationService service = serviceWith(new DiscordGuildMember("1\uAE30 John", List.of("graduate")));

        DiscordVerificationResult result = service.verify(UUID.randomUUID(), token());

        assertTrue(result.success());
        assertEquals(RoleType.GRADUATE, result.user().roleType());
        assertEquals(1, result.user().flag());
        assertEquals("John", result.user().name());
    }

    @Test
    void verifiesStudentNickname() throws Exception {
        DiscordVerificationService service = serviceWith(new DiscordGuildMember("1234 John", List.of("student")));

        DiscordVerificationResult result = service.verify(UUID.randomUUID(), token());

        assertTrue(result.success());
        assertEquals(RoleType.STUDENT, result.user().roleType());
        assertEquals("1234", result.user().studentId());
        assertEquals("John", result.user().name());
    }

    @Test
    void rejectsInvalidNickname() throws Exception {
        DiscordVerificationService service = serviceWith(new DiscordGuildMember("John", List.of("student")));

        DiscordVerificationResult result = service.verify(UUID.randomUUID(), token());

        assertFalse(result.success());
    }

    @Test
    void rejectsGraduateNicknameWithoutFlagSuffix() throws Exception {
        DiscordVerificationService service = serviceWith(new DiscordGuildMember("1 John", List.of("graduate")));

        DiscordVerificationResult result = service.verify(UUID.randomUUID(), token());

        assertFalse(result.success());
    }

    @Test
    void rejectsUserOutsideConfiguredGuild() throws Exception {
        DiscordVerificationService service = new DiscordVerificationService(
            TestConfigs.verifyConfig(),
            new FakeDiscordClient(new DiscordGuildMember("1234 John", List.of("student")), false)
        );

        DiscordVerificationResult result = service.verify(UUID.randomUUID(), token());

        assertFalse(result.success());
    }

    private DiscordVerificationService serviceWith(DiscordGuildMember member) {
        return new DiscordVerificationService(TestConfigs.verifyConfig(), new FakeDiscordClient(member, true));
    }

    private DiscordTokenResponse token() {
        return new DiscordTokenResponse("access", "Bearer", 300, "identify guilds guilds.members.read");
    }

    private record FakeDiscordClient(DiscordGuildMember member, boolean joinedGuild) implements DiscordClient {
        @Override
        public DiscordUser fetchCurrentUser(String accessToken) {
            return new DiscordUser("discord", "user", "global");
        }

        @Override
        public List<DiscordGuild> fetchCurrentUserGuilds(String accessToken) {
            return joinedGuild ? List.of(new DiscordGuild("guild", "GSM")) : List.of();
        }

        @Override
        public DiscordGuildMember fetchCurrentUserGuildMember(String accessToken) {
            return member;
        }
    }
}
