package dev.hoi.protocol;

import com.google.gson.Gson;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public final class ResearchProtocol {
    private static final Gson JSON = new Gson();
    private static boolean registered;
    private ResearchProtocol() {}
    public static final int MAX_JSON = 200_000;
    public enum Action { OPEN, START, CANCEL, CLOSE }
    /** Server command asks the client to use the normal authenticated OPEN request. */
    public record OpenScreen() implements CustomPacketPayload {
        public static final Type<OpenScreen> TYPE = new Type<>(Identifier.fromNamespaceAndPath("hoi", "research_open_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenScreen> CODEC = StreamCodec.unit(new OpenScreen());
        @Override public Type<OpenScreen> type() { return TYPE; }
    }
    public record Request(Action action, String session, long revision, int slot, String technology) implements CustomPacketPayload {
        public static final Type<Request> TYPE = new Type<>(Identifier.fromNamespaceAndPath("hoi", "research_request_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Request> CODEC = StreamCodec.of(
                (b, p) -> { b.writeEnum(p.action); b.writeUtf(p.session, 36); b.writeVarLong(p.revision); b.writeVarInt(p.slot); b.writeUtf(p.technology, 160); },
                b -> new Request(b.readEnum(Action.class), b.readUtf(36), b.readVarLong(), b.readVarInt(), b.readUtf(160)));
        @Override public Type<Request> type() { return TYPE; }
    }
    public record Response(String json) implements CustomPacketPayload {
        public static final Type<Response> TYPE = new Type<>(Identifier.fromNamespaceAndPath("hoi", "research_view_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Response> CODEC = StreamCodec.of(
                (b, p) -> b.writeUtf(p.json, MAX_JSON), b -> new Response(b.readUtf(MAX_JSON)));
        public Response {
            if (json == null || json.length() > MAX_JSON) throw new IllegalArgumentException("연구 화면 데이터 한도 초과");
        }
        public static Response of(ResearchView view) { return new Response(JSON.toJson(view)); }
        public ResearchView view() { return JSON.fromJson(json, ResearchView.class); }
        @Override public Type<Response> type() { return TYPE; }
    }
    /** Consumers call this before their receivers; Fabric does not order independent mod entrypoints. */
    public static synchronized void registerPayloadTypes() {
        if (registered) return;
        PayloadTypeRegistry.serverboundPlay().register(Request.TYPE, Request.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Response.TYPE, Response.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(OpenScreen.TYPE, OpenScreen.CODEC);
        registered = true;
    }
}
