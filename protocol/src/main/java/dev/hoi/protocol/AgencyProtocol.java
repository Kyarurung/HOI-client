package dev.hoi.protocol;

import com.google.gson.Gson;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public final class AgencyProtocol {
    private static final Gson JSON = new Gson();
    public static final int MAX_JSON = 400_000;
    private AgencyProtocol() {}
    public enum Kind { OPEN, REFRESH, CALL, CLOSE }
    public record Request(Kind kind, String session, long revision, String item, List<String> arguments) implements CustomPacketPayload {
        public Request {
            if (kind == null || revision < 0) throw new IllegalArgumentException("Invalid agency request");
            AgencyView.text(session, 36); AgencyView.text(item, 128); arguments = List.copyOf(arguments);
            if (arguments.size() > 4) throw new IllegalArgumentException("Too many arguments");
            arguments.forEach(a -> AgencyView.text(a, 128));
        }
        public static final Type<Request> TYPE = new Type<>(Identifier.fromNamespaceAndPath("hoi", "agency_request_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Request> CODEC = StreamCodec.of(
                (b, p) -> { b.writeEnum(p.kind); b.writeUtf(p.session, 36); b.writeVarLong(p.revision); b.writeUtf(p.item, 128);
                    b.writeCollection(p.arguments, (out, value) -> out.writeUtf(value, 128)); },
                b -> new Request(b.readEnum(Kind.class), b.readUtf(36), b.readVarLong(), b.readUtf(128),
                        b.readCollection(net.minecraft.network.FriendlyByteBuf.limitValue(java.util.ArrayList::new, 4), in -> in.readUtf(128))));
        @Override public Type<Request> type() { return TYPE; }
    }
    public record Response(String json) implements CustomPacketPayload {
        public Response { AgencyView.text(json, MAX_JSON); }
        public static final Type<Response> TYPE = new Type<>(Identifier.fromNamespaceAndPath("hoi", "agency_response_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Response> CODEC = StreamCodec.of(
                (b, p) -> b.writeUtf(p.json, MAX_JSON), b -> new Response(b.readUtf(MAX_JSON)));
        public static Response of(AgencyView view) { return new Response(JSON.toJson(view)); }
        public AgencyView view() {
            var view = JSON.fromJson(json, AgencyView.class);
            if (view == null) throw new IllegalArgumentException("Missing agency view");
            return view;
        }
        @Override public Type<Response> type() { return TYPE; }
    }
}
