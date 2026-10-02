package dev.hoi.protocol;

import com.google.gson.Gson;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public final class ProtocolDoctrinePayloads {
    private static final Gson JSON = new Gson();
    public static final int MAX_JSON = 400_000;
    private static boolean registered;
    private ProtocolDoctrinePayloads() {}
    public enum Kind { OPEN, REFRESH, SELECT_GRAND, SELECT_SUBDOCTRINE, CLOSE }
    public record Request(Kind kind, String session, long revision, String folder, String track, String item) implements CustomPacketPayload {
        public Request {
            if (kind == null || revision < 0) throw new IllegalArgumentException("Invalid doctrine request");
            ProtocolDoctrineView.text(session, 36); ProtocolDoctrineView.text(folder, 32);
            ProtocolDoctrineView.text(track, 96); ProtocolDoctrineView.text(item, 96);
        }
        public static final Type<Request> TYPE = new Type<>(Identifier.fromNamespaceAndPath("hoi", "doctrine_request_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Request> CODEC = StreamCodec.of(
            (b, p) -> { b.writeEnum(p.kind); b.writeUtf(p.session, 36); b.writeVarLong(p.revision); b.writeUtf(p.folder, 32); b.writeUtf(p.track, 96); b.writeUtf(p.item, 96); },
            b -> new Request(b.readEnum(Kind.class), b.readUtf(36), b.readVarLong(), b.readUtf(32), b.readUtf(96), b.readUtf(96)));
        @Override public Type<Request> type() { return TYPE; }
    }
    public record Response(String json) implements CustomPacketPayload {
        public Response { ProtocolDoctrineView.text(json, MAX_JSON); }
        public static final Type<Response> TYPE = new Type<>(Identifier.fromNamespaceAndPath("hoi", "doctrine_response_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Response> CODEC = StreamCodec.of(
            (b, p) -> b.writeUtf(p.json, MAX_JSON), b -> new Response(b.readUtf(MAX_JSON)));
        public static Response of(ProtocolDoctrineView view) { return new Response(JSON.toJson(view)); }
        public ProtocolDoctrineView view() {
            try { return ProtocolJsonCodec.read(JSON, json, ProtocolDoctrineView.class); }
            catch (RuntimeException invalid) { throw new IllegalArgumentException("Invalid doctrine response", invalid); }
        }
        @Override public Type<Response> type() { return TYPE; }
    }
    public static synchronized void registerPayloadTypes() {
        if (registered) return;
        PayloadTypeRegistry.serverboundPlay().register(Request.TYPE, Request.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Response.TYPE, Response.CODEC);
        registered = true;
    }
}
