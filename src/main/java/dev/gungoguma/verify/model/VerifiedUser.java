package dev.gungoguma.verify.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class VerifiedUser {
    private final UUID uuid;
    private final String discordId;
    private final String name;
    private final Integer flag;
    private final String studentId;
    private final RoleType roleType;
    private final Instant verifiedAt;

    public VerifiedUser(
        UUID uuid,
        String discordId,
        String name,
        Integer flag,
        String studentId,
        RoleType roleType,
        Instant verifiedAt
    ) {
        this.uuid = Objects.requireNonNull(uuid, "uuid");
        this.discordId = Objects.requireNonNull(discordId, "discordId");
        this.name = Objects.requireNonNull(name, "name");
        this.flag = flag;
        this.studentId = studentId;
        this.roleType = Objects.requireNonNull(roleType, "roleType");
        this.verifiedAt = Objects.requireNonNull(verifiedAt, "verifiedAt");
    }

    public UUID uuid() {
        return uuid;
    }

    public String discordId() {
        return discordId;
    }

    public String name() {
        return name;
    }

    public Integer flag() {
        return flag;
    }

    public String studentId() {
        return studentId;
    }

    public RoleType roleType() {
        return roleType;
    }

    public Instant verifiedAt() {
        return verifiedAt;
    }
}
