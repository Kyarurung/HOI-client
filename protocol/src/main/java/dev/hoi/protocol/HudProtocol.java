package dev.hoi.protocol;

import com.google.gson.Gson;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public final class HudProtocol {
    private static final Gson JSON = new Gson();
    private static boolean registered;
    private HudProtocol() {}
    public static synchronized void registerPayloadTypes() {
        if (registered) return;
        PayloadTypeRegistry.clientboundPlay().register(State.TYPE, State.CODEC);
        registered = true;
    }
    public record State(String country, String json) implements CustomPacketPayload {
        public static final State HIDDEN = new State("", "");
        public State {
            if (country == null || json == null || json.length() > 50_000) throw new IllegalArgumentException("Invalid HUD snapshot");
            if (!country.isEmpty()) CountryView.tag(country);
            if (country.isEmpty() != json.isEmpty()) throw new IllegalArgumentException("HUD visibility mismatch");
        }
        public static final Type<State> TYPE = new Type<>(Identifier.fromNamespaceAndPath("hoi", "hud_state_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf, State> CODEC = StreamCodec.of(
                (b,p) -> { b.writeUtf(p.country,16); b.writeUtf(p.json,50_000); }, b -> new State(b.readUtf(16),b.readUtf(50_000)));
        public static State of(String country, CountryHud hud) { return new State(country, JSON.toJson(hud)); }
        public CountryHud hud() {
            if (country.isEmpty()) return CountryHud.UNKNOWN;
            var hud = PayloadJson.read(JSON, json, CountryHud.class);
            if (hud == null) throw new IllegalArgumentException("Missing HUD data");
            return hud;
        }
        @Override public Type<State> type() { return TYPE; }
    }
}
