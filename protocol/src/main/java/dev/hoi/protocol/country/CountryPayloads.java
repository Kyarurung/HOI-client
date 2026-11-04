package dev.hoi.protocol.country;

import dev.hoi.protocol.ProtocolJsonCodec;
import com.google.gson.Gson;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public final class CountryPayloads {
    private static final Gson JSON = new Gson();
    private static boolean registered;
    private CountryPayloads() {}
    public static synchronized void registerPayloadTypes() {
        if (registered) return;
        PayloadTypeRegistry.serverboundPlay().register(Request.TYPE, Request.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(Action.TYPE, Action.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Response.TYPE, Response.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(OpenScreen.TYPE, OpenScreen.CODEC);
        CountryDisplay.registerPayloadTypes();
        registered = true;
    }
    private static void token(String value) { CountryView.text(value, 36); java.util.UUID.fromString(value); }
    public record Action(String token, String session, long revision, String id, long amount) implements CustomPacketPayload {
        public Action(String token,String session,long revision,String id) {this(token,session,revision,id,0);}
        public Action { CountryPayloads.token(token); CountryPayloads.token(session); CountryView.text(id, 64); if (revision < 0 || amount < 0) throw new IllegalArgumentException("Invalid revision or amount"); }
        public static final Type<Action> TYPE = new Type<>(Identifier.fromNamespaceAndPath("hoi", "country_action_v2"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Action> CODEC = StreamCodec.of(
                (b,p) -> { b.writeUtf(p.token,36); b.writeUtf(p.session,36); b.writeLong(p.revision); b.writeUtf(p.id,64); b.writeLong(p.amount); },
                b -> new Action(b.readUtf(36), b.readUtf(36), b.readLong(), b.readUtf(64), b.readLong()));
        @Override public Type<Action> type() { return TYPE; }
    }
    public record Request(String token, String target) implements CustomPacketPayload {
        public Request { CountryPayloads.token(token); if (!target.isEmpty()) CountryView.tag(target); }
        public static final Type<Request> TYPE = new Type<>(Identifier.fromNamespaceAndPath("hoi", "country_request_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Request> CODEC = StreamCodec.of(
                (b,p) -> { b.writeUtf(p.token,36); b.writeUtf(p.target,16); }, b -> new Request(b.readUtf(36),b.readUtf(16)));
        @Override public Type<Request> type() { return TYPE; }
    }
    public record Response(String token, String json) implements CustomPacketPayload {
        public Response { CountryPayloads.token(token); CountryView.text(json, 200_000); }
        public static final Type<Response> TYPE = new Type<>(Identifier.fromNamespaceAndPath("hoi", "country_response_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Response> CODEC = StreamCodec.of(
                (b,p) -> { b.writeUtf(p.token,36); b.writeUtf(p.json,200_000); }, b -> new Response(b.readUtf(36),b.readUtf(200_000)));
        public static Response of(String token, CountryView view) { return new Response(token, JSON.toJson(view)); }
        public CountryView view() { return ProtocolJsonCodec.read(JSON, json, CountryView.class); }
        @Override public Type<Response> type() { return TYPE; }
    }
    public record OpenScreen(String target) implements CustomPacketPayload {
        public OpenScreen { CountryView.tag(target); }
        public static final Type<OpenScreen> TYPE = new Type<>(Identifier.fromNamespaceAndPath("hoi", "country_open_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenScreen> CODEC = StreamCodec.of((b,p) -> b.writeUtf(p.target,16),b -> new OpenScreen(b.readUtf(16)));
        @Override public Type<OpenScreen> type() { return TYPE; }
    }
}
