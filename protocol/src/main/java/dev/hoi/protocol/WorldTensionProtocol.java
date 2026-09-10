package dev.hoi.protocol;

import com.google.gson.Gson;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import java.util.Objects;
import java.util.UUID;

public final class WorldTensionProtocol {
    private static final Gson JSON = new Gson();
    private static boolean registered;
    private WorldTensionProtocol() {}
    public static synchronized void registerPayloadTypes() {
        if (registered) return;
        PayloadTypeRegistry.serverboundPlay().register(Request.TYPE, Request.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Response.TYPE, Response.CODEC);
        registered = true;
    }
    private static void token(String value) { CountryView.text(value, 36); UUID.fromString(value); }
    public record Request(String token, WorldTensionView.Sort sort, boolean descending, int offset) implements CustomPacketPayload {
        public Request {
            WorldTensionProtocol.token(token); Objects.requireNonNull(sort);
            if (offset < 0 || offset > 10_000_000) throw new IllegalArgumentException("Invalid offset");
        }
        public static final Type<Request> TYPE = new Type<>(Identifier.fromNamespaceAndPath("hoi", "world_tension_request_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Request> CODEC = StreamCodec.of(
                (b,p) -> { b.writeUtf(p.token,36); b.writeEnum(p.sort); b.writeBoolean(p.descending); b.writeVarInt(p.offset); },
                b -> new Request(b.readUtf(36), b.readEnum(WorldTensionView.Sort.class), b.readBoolean(), b.readVarInt()));
        @Override public Type<Request> type() { return TYPE; }
    }
    public record Response(String token, String json) implements CustomPacketPayload {
        public Response { WorldTensionProtocol.token(token); CountryView.text(json, 900_000); }
        public static final Type<Response> TYPE = new Type<>(Identifier.fromNamespaceAndPath("hoi", "world_tension_response_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Response> CODEC = StreamCodec.of(
                (b,p) -> { b.writeUtf(p.token,36); b.writeUtf(p.json,900_000); }, b -> new Response(b.readUtf(36),b.readUtf(900_000)));
        public static Response of(String token, WorldTensionView view) { return new Response(token, JSON.toJson(view)); }
        public WorldTensionView view() { return PayloadJson.read(JSON, json, WorldTensionView.class); }
        @Override public Type<Response> type() { return TYPE; }
    }
}
