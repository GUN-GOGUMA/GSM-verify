package dev.gungoguma.verify.notify;

import dev.gungoguma.verify.model.RoleType;
import dev.gungoguma.verify.model.VerifiedUser;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record QueueNotifyPayload(
    QueueNotifyType type,
    UUID uuid,
    String name,
    RoleType roleType,
    String studentId,
    Integer flag,
    Instant timestamp
) {
    public QueueNotifyPayload {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(uuid, "uuid");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(roleType, "roleType");
        Objects.requireNonNull(timestamp, "timestamp");
    }

    public static QueueNotifyPayload verifySuccess(VerifiedUser user) {
        return new QueueNotifyPayload(
            QueueNotifyType.VERIFY_SUCCESS,
            user.uuid(),
            user.name(),
            user.roleType(),
            user.studentId(),
            user.flag(),
            user.verifiedAt()
        );
    }

    public static QueueNotifyPayload verifyReset(VerifiedUser user, Instant timestamp) {
        return new QueueNotifyPayload(
            QueueNotifyType.VERIFY_RESET,
            user.uuid(),
            user.name(),
            user.roleType(),
            user.studentId(),
            user.flag(),
            timestamp
        );
    }
}
