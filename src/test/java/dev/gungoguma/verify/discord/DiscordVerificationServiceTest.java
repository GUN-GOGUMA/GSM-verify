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
        DiscordVerificationService service = serviceWith(new DiscordGuildMember("1기 김검증", List.of("graduate")));

        DiscordVerificationResult result = service.verify(UUID.randomUUID(), token());

        assertTrue(result.success());
        assertEquals(RoleType.GRADUATE, result.user().roleType());
        assertEquals(1, result.user().flag());
        assertEquals("김검증", result.user().name());
    }

    @Test
    void verifiesStudentNickname() throws Exception {
        DiscordVerificationService service = serviceWith(new DiscordGuildMember("1234 김검증", List.of("student")));

        DiscordVerificationResult result = service.verify(UUID.randomUUID(), token());

        assertTrue(result.success());
        assertEquals(RoleType.STUDENT, result.user().roleType());
        assertEquals("1234", result.user().studentId());
        assertEquals("김검증", result.user().name());
    }

    @Test
    void rejectsInvalidNickname() throws Exception {
        DiscordVerificationService service = serviceWith(new DiscordGuildMember("김검증", List.of("student")));

        DiscordVerificationResult result = service.verify(UUID.randomUUID(), token());

        assertFalse(result.success());
    }

    private DiscordVerificationService serviceWith(DiscordGuildMember member) {
        return new DiscordVerificationService(TestConfigs.verifyConfig(), new FakeDiscordClient(member));
    }

    private DiscordTokenResponse token() {
        return new DiscordTokenResponse("access", "Bearer", 300, "identify guilds guilds.members.read");
    }

    private record FakeDiscordClient(DiscordGuildMember member) implements DiscordClient {
        @Override
        public DiscordUser fetchCurrentUser(String accessToken) {
            return new DiscordUser("discord", "user", "global");
        }

        @Override
        public DiscordGuildMember fetchCurrentUserGuildMember(String accessToken) {
            return member;
        }
    }
}
