package dev.hoi.protocol;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

public final class ProtocolJsonCodec {
    private ProtocolJsonCodec() {}
    public static <T> T read(Gson json, String source, Class<T> type) {
        try {
            var value = json.fromJson(source, type);
            if (value == null) throw new IllegalArgumentException("Missing " + type.getSimpleName());
            return value;
        } catch (JsonParseException | IllegalStateException error) {
            throw new IllegalArgumentException("Invalid " + type.getSimpleName(), error);
        }
    }
}
