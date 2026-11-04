package dev.hoi.protocol.agency;

import dev.hoi.protocol.ProtocolJsonCodec;
import dev.hoi.protocol.map.MapAtlasScene;
import com.google.gson.Gson;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public final class AgencyPayloads {
    private static final Gson JSON = new Gson();
    public static final int MAX_JSON = 400_000;
    private AgencyPayloads() {}
    public enum Kind { OPEN, REFRESH, CALL, CLOSE, HIGHLIGHT }
    public record Request(Kind kind, String session, long revision, String item, List<String> arguments) implements CustomPacketPayload {
        private static final StreamCodec<io.netty.buffer.ByteBuf, List<String>> ARGUMENTS =
                ByteBufCodecs.stringUtf8(128).apply(ByteBufCodecs.list(4));
        public Request {
            if (kind == null || revision < 0) throw new IllegalArgumentException("Invalid agency request");
            AgencyView.text(session, 36); AgencyView.text(item, 128); arguments = List.copyOf(arguments);
            if (arguments.size() > 4) throw new IllegalArgumentException("Too many arguments");
            arguments.forEach(a -> AgencyView.text(a, 128));
        }
        public static final Type<Request> TYPE = new Type<>(Identifier.fromNamespaceAndPath("hoi", "agency_request_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Request> CODEC = StreamCodec.of(
                (b, p) -> { b.writeEnum(p.kind); b.writeUtf(p.session, 36); b.writeVarLong(p.revision); b.writeUtf(p.item, 128);
                    ARGUMENTS.encode(b, p.arguments); },
                b -> new Request(b.readEnum(Kind.class), b.readUtf(36), b.readVarLong(), b.readUtf(128),
                        ARGUMENTS.decode(b)));
        @Override public Type<Request> type() { return TYPE; }
    }
    public record Response(String json) implements CustomPacketPayload {
        public Response { AgencyView.text(json, MAX_JSON); }
        public static final Type<Response> TYPE = new Type<>(Identifier.fromNamespaceAndPath("hoi", "agency_response_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Response> CODEC = StreamCodec.of(
                (b, p) -> b.writeUtf(p.json, MAX_JSON), b -> new Response(b.readUtf(MAX_JSON)));
        public static Response of(AgencyView view) { return new Response(JSON.toJson(view)); }
        public AgencyView view() { return ProtocolJsonCodec.read(JSON, json, AgencyView.class); }
        @Override public Type<Response> type() { return TYPE; }
    }
    public record Highlight(String session, long revision, String item, MapAtlasScene.Page page) implements CustomPacketPayload {
        public Highlight {
            if (revision < 0 || page == null) throw new IllegalArgumentException("Invalid agency highlight");
            AgencyView.text(session, 36); AgencyView.text(item, 128);
        }
        public static final Type<Highlight> TYPE = new Type<>(Identifier.fromNamespaceAndPath("hoi", "agency_highlight_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Highlight> CODEC = StreamCodec.of(
                (b, p) -> { b.writeUtf(p.session, 36); b.writeVarLong(p.revision); b.writeUtf(p.item, 128); MapAtlasScene.Page.CODEC.encode(b, p.page); },
                b -> new Highlight(b.readUtf(36), b.readVarLong(), b.readUtf(128), MapAtlasScene.Page.CODEC.decode(b)));
        @Override public Type<Highlight> type() { return TYPE; }
    }
}
