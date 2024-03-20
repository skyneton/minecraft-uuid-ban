package net.mpoisv.ban.utils;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.UUID;

public class UUIDTypeAdapter extends TypeAdapter<UUID> {
    public void write(final JsonWriter out, final UUID value) throws IOException {
        out.value(fromUUID(value));
    }

    public UUID read(final JsonReader in) throws IOException {
        return fromString(in.nextString());
    }

    private static String fromUUID(final UUID value) {
        return value.toString().replace("-", "");
    }

    public static UUID fromString(final String uuid) {
        if(uuid.contains("-")) return UUID.fromString(uuid);
        if(uuid.length() != 32) return null;
        var builder = new StringBuilder();
        builder.append(uuid.substring(8))
                .append('-')
                .append(uuid, 8, 12)
                .append('-')
                .append(uuid, 12, 16)
                .append('-')
                .append(uuid, 16, 20)
                .append('-')
                .append(uuid, 20, 36);
        return UUID.fromString(builder.toString());
    }
}
