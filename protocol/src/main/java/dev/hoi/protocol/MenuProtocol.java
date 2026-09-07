package dev.hoi.protocol;

import com.google.gson.Gson;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Additive payload; existing research payload IDs and wire layouts remain unchanged. */
public final class MenuProtocol {
    public static final int MAX_JSON = 200_000;
    private static final Gson JSON = new Gson();
    private MenuProtocol() {}

    /** Read-only refresh: the server resolves the player country, never a supplied tag. */
    public record Refresh(String screen) implements CustomPacketPayload {
        public Refresh { if (screen == null || screen.length() > 36) throw new IllegalArgumentException("Invalid screen token"); }
        public static final Type<Refresh> TYPE = new Type<>(Identifier.fromNamespaceAndPath("hoi", "menu_refresh_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Refresh> CODEC = StreamCodec.of(
                (b, p) -> b.writeUtf(p.screen, 36), b -> new Refresh(b.readUtf(36)));
        @Override public Type<Refresh> type() { return TYPE; }
    }
    /** Empty JSON revokes an open view when its country is no longer authorized. */
    public record Update(String screen, String json) implements CustomPacketPayload {
        public Update { new Refresh(screen); new OpenScreen(json); }
        public static final Type<Update> TYPE = new Type<>(Identifier.fromNamespaceAndPath("hoi", "menu_update_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Update> CODEC = StreamCodec.of(
                (b, p) -> { b.writeUtf(p.screen, 36); b.writeUtf(p.json, MAX_JSON); }, b -> new Update(b.readUtf(36), b.readUtf(MAX_JSON)));
        @Override public Type<Update> type() { return TYPE; }
    }

    public record OpenScreen(String json) implements CustomPacketPayload {
        public static final Type<OpenScreen> TYPE = new Type<>(Identifier.fromNamespaceAndPath("hoi", "menu_open_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenScreen> CODEC = StreamCodec.of(
                (buffer, value) -> buffer.writeUtf(value.json, MAX_JSON), buffer -> new OpenScreen(buffer.readUtf(MAX_JSON)));
        public OpenScreen {
            if (json == null || json.length() > MAX_JSON) throw new IllegalArgumentException("메뉴 데이터 한도 초과");
        }
        public static OpenScreen of(MenuView view) { return new OpenScreen(JSON.toJson(view)); }
        public MenuView view() {
            var view = JSON.fromJson(json, MenuView.class);
            if (view == null) throw new IllegalArgumentException("Invalid menu snapshot");
            return view;
        }
        @Override public Type<OpenScreen> type() { return TYPE; }
    }
}
