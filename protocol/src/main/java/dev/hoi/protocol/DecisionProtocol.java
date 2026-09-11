package dev.hoi.protocol;

import com.google.gson.Gson;
import java.util.*;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public final class DecisionProtocol {
    public static final int PAGE_SIZE = 48, MAX_JSON = 160_000;
    private static final Gson JSON = new Gson();
    private static boolean registered;
    private DecisionProtocol() {}
    public static synchronized void registerPayloadTypes() {
        if (registered) return;
        PayloadTypeRegistry.serverboundPlay().register(Request.TYPE, Request.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Response.TYPE, Response.CODEC);
        registered = true;
    }
    public enum Action { OPEN, REFRESH, TAKE }
    public record Request(Action action, String token, String session, long revision, String id, int page) implements CustomPacketPayload {
        public Request {
            Objects.requireNonNull(action); UUID.fromString(token);
            if (!session.isEmpty()) UUID.fromString(session);
            CountryView.text(id, 256);
            if (revision < 0 || page < 0 || page > 256) throw new IllegalArgumentException("Invalid decision request");
        }
        public static final Type<Request> TYPE = new Type<>(Identifier.parse("hoi:decision_request_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Request> CODEC = StreamCodec.of(
                (b,p) -> { b.writeEnum(p.action); b.writeUtf(p.token,36); b.writeUtf(p.session,36); b.writeVarLong(p.revision); b.writeUtf(p.id,256); b.writeVarInt(p.page); },
                b -> new Request(b.readEnum(Action.class),b.readUtf(36),b.readUtf(36),b.readVarLong(),b.readUtf(256),b.readVarInt()));
        @Override public Type<Request> type() { return TYPE; }
    }
    public record Row(String id, String category, String categoryName, String categoryIcon, String name, String description, String icon,
            double cost, long remainingHours, String status, boolean enabled) {
        public Row {
            for (String text : List.of(id,category,categoryName,categoryIcon,name,icon,status)) CountryView.text(text,256);
            CountryView.text(description,1600);
            if (!Double.isFinite(cost) || cost < 0 || cost > 1000000 || remainingHours < 0) throw new IllegalArgumentException("Invalid decision row");
        }
    }
    public record View(String country, String session, long revision, String message, int page, int total,
            CountryHud hud, List<Row> rows) {
        public View {
            CountryView.tag(country); UUID.fromString(session); CountryView.text(message,512); Objects.requireNonNull(hud);
            rows = List.copyOf(rows);
            if (revision < 0 || page < 0 || page > 256 || total < 0 || total > 12288 || rows.size() > PAGE_SIZE)
                throw new IllegalArgumentException("Invalid decision view");
            if (rows.stream().map(Row::id).distinct().count() != rows.size()) throw new IllegalArgumentException("Duplicate decision row");
        }
    }
    public record Response(String token, String json) implements CustomPacketPayload {
        public Response { UUID.fromString(token); CountryView.text(json,MAX_JSON); }
        public static final Type<Response> TYPE = new Type<>(Identifier.parse("hoi:decision_response_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Response> CODEC = StreamCodec.of(
                (b,p) -> { b.writeUtf(p.token,36); b.writeUtf(p.json,MAX_JSON); }, b -> new Response(b.readUtf(36),b.readUtf(MAX_JSON)));
        public static Response of(String token, View view) { return new Response(token,JSON.toJson(view)); }
        public View view() { return PayloadJson.read(JSON,json,View.class); }
        @Override public Type<Response> type() { return TYPE; }
    }
}
