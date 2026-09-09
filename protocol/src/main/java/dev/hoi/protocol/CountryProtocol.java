package dev.hoi.protocol;

import com.google.gson.Gson;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public final class CountryProtocol {
    private static final Gson JSON = new Gson();
    private static boolean registered;
    private CountryProtocol() {}
    public static synchronized void registerPayloadTypes() {
        if (registered) return;
        PayloadTypeRegistry.serverboundPlay().register(Request.TYPE, Request.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Response.TYPE, Response.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(OpenScreen.TYPE, OpenScreen.CODEC);
        registered = true;
    }
    private static void token(String value) { CountryView.text(value, 36); java.util.UUID.fromString(value); }
    public record Request(String token, String target) implements CustomPacketPayload {
        public Request { CountryProtocol.token(token); if (!target.isEmpty()) CountryView.tag(target); }
        public static final Type<Request> TYPE = new Type<>(Identifier.fromNamespaceAndPath("hoi", "country_request_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Request> CODEC = StreamCodec.of(
                (b,p) -> { b.writeUtf(p.token,36); b.writeUtf(p.target,16); }, b -> new Request(b.readUtf(36),b.readUtf(16)));
        @Override public Type<Request> type() { return TYPE; }
    }
    public record Response(String token, String json) implements CustomPacketPayload {
        public Response { CountryProtocol.token(token); CountryView.text(json, 200_000); }
        public static final Type<Response> TYPE = new Type<>(Identifier.fromNamespaceAndPath("hoi", "country_response_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Response> CODEC = StreamCodec.of(
                (b,p) -> { b.writeUtf(p.token,36); b.writeUtf(p.json,200_000); }, b -> new Response(b.readUtf(36),b.readUtf(200_000)));
        public static Response of(String token, CountryView view) { return new Response(token, JSON.toJson(view)); }
        public CountryView view() { var view = JSON.fromJson(json, CountryView.class); if (view == null) throw new IllegalArgumentException("Missing country view"); return view; }
        @Override public Type<Response> type() { return TYPE; }
    }

    public record OpenScreen(String target) implements CustomPacketPayload {
        public OpenScreen { CountryView.tag(target); }
        public static final Type<OpenScreen> TYPE = new Type<>(Identifier.fromNamespaceAndPath("hoi", "country_open_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenScreen> CODEC = StreamCodec.of((b,p) -> b.writeUtf(p.target,16),b -> new OpenScreen(b.readUtf(16)));
        @Override public Type<OpenScreen> type() { return TYPE; }
    }
}
