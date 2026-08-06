package dev.gungoguma.verify.notify;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import dev.gungoguma.verify.model.RoleType;
import java.time.Instant;
import java.util.UUID;

public final class QueueNotifyPayloadCodec {
    public static final String CHANNEL = "gsm:notify";
    private static final int VERSION = 1;

    private QueueNotifyPayloadCodec() {
    }

    public static byte[] encode(QueueNotifyPayload payload) {
        ByteArrayDataOutput output = ByteStreams.newDataOutput();
        output.writeInt(VERSION);
        output.writeUTF(payload.type().name());
        output.writeUTF(payload.uuid().toString());
        output.writeUTF(payload.name());
        output.writeUTF(payload.roleType().name());
        output.writeUTF(payload.studentId() == null ? "" : payload.studentId());
        output.writeUTF(payload.flag() == null ? "" : payload.flag().toString());
        output.writeUTF(payload.timestamp().toString());
        return output.toByteArray();
    }

    public static QueueNotifyPayload decode(byte[] bytes) {
        ByteArrayDataInput input = ByteStreams.newDataInput(bytes);
        int version = input.readInt();
        if (version != VERSION) {
            throw new IllegalArgumentException("Unsupported queue notify payload version: " + version);
        }

        return new QueueNotifyPayload(
            QueueNotifyType.valueOf(input.readUTF()),
            UUID.fromString(input.readUTF()),
            input.readUTF(),
            RoleType.valueOf(input.readUTF()),
            blankToNull(input.readUTF()),
            parseFlag(input.readUTF()),
            Instant.parse(input.readUTF())
        );
    }

    private static String blankToNull(String value) {
        return value.isBlank() ? null : value;
    }

    private static Integer parseFlag(String value) {
        return value.isBlank() ? null : Integer.parseInt(value);
    }
}
