package dev.gungoguma.verify.notify;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import dev.gungoguma.verify.model.RoleType;
import dev.gungoguma.verify.model.VerifiedUser;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class QueueNotifyPayloadCodecTest {
    @Test
    void encodesAndDecodesStudentVerifySuccessPayload() {
        VerifiedUser user = new VerifiedUser(
            UUID.fromString("00000000-0000-0000-0000-000000000001"),
            "1234",
            "홍길동",
            null,
            "2601",
            RoleType.STUDENT,
            Instant.parse("2026-08-06T11:00:00Z")
        );

        QueueNotifyPayload decoded = QueueNotifyPayloadCodec.decode(
            QueueNotifyPayloadCodec.encode(QueueNotifyPayload.verifySuccess(user))
        );

        assertEquals(QueueNotifyType.VERIFY_SUCCESS, decoded.type());
        assertEquals(user.uuid(), decoded.uuid());
        assertEquals(user.name(), decoded.name());
        assertEquals(RoleType.STUDENT, decoded.roleType());
        assertEquals("2601", decoded.studentId());
        assertNull(decoded.flag());
        assertEquals(user.verifiedAt(), decoded.timestamp());
    }

    @Test
    void encodesAndDecodesGraduateResetPayload() {
        VerifiedUser user = new VerifiedUser(
            UUID.fromString("00000000-0000-0000-0000-000000000002"),
            "5678",
            "김구름",
            8,
            null,
            RoleType.GRADUATE,
            Instant.parse("2026-08-06T12:00:00Z")
        );

        QueueNotifyPayload decoded = QueueNotifyPayloadCodec.decode(
            QueueNotifyPayloadCodec.encode(QueueNotifyPayload.verifyReset(
                user,
                Instant.parse("2027-01-12T00:00:00Z")
            ))
        );

        assertEquals(QueueNotifyType.VERIFY_RESET, decoded.type());
        assertEquals(user.uuid(), decoded.uuid());
        assertEquals(user.name(), decoded.name());
        assertEquals(RoleType.GRADUATE, decoded.roleType());
        assertNull(decoded.studentId());
        assertEquals(8, decoded.flag());
        assertEquals(Instant.parse("2027-01-12T00:00:00Z"), decoded.timestamp());
    }
}
