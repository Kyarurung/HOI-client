package dev.hoi.protocol.diplomacy;

import dev.hoi.protocol.ProtocolJsonCodec;
import dev.hoi.protocol.country.CountryView;
import com.google.gson.Gson;
import java.util.Objects;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public final class DiplomacyVolunteerPayloads {
    public static final int MAX_JSON = 200_000;
    private static final Gson JSON = new Gson();
    private static boolean registered;
    private DiplomacyVolunteerPayloads() {}
    public enum Operation { OPEN, SELECT, AIR, SUBMIT, CANCEL }
    public enum Status { OPEN, UPDATED, COMPLETED, REVOKED }
    public static synchronized void registerPayloadTypes() {
        if (registered) return;
        PayloadTypeRegistry.serverboundPlay().register(Request.TYPE, Request.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Response.TYPE, Response.CODEC);
        registered = true;
    }
    public record Request(String token, String session, long revision, Operation operation, String key,
            boolean selected) implements CustomPacketPayload {
        public Request {
            DiplomacyVolunteerView.token(token);
            DiplomacyVolunteerView.token(session);
            if (revision < 0 || operation == null) throw new IllegalArgumentException("Invalid volunteer request");
            CountryView.text(key, 64);
            if (operation == Operation.SELECT) {
                DiplomacyVolunteerView.key(key);
            } else if (!key.isEmpty() || operation != Operation.AIR && selected) {
                throw new IllegalArgumentException("Unexpected volunteer selection");
            }
        }
        public static final Type<Request> TYPE = new Type<>(Identifier.fromNamespaceAndPath("hoi", "diplomacy_volunteer_request_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Request> CODEC = StreamCodec.of(
                (b, p) -> {
                    b.writeUtf(p.token, 36);
                    b.writeUtf(p.session, 36);
                    b.writeVarLong(p.revision);
                    b.writeEnum(p.operation);
                    b.writeUtf(p.key, 64);
                    b.writeBoolean(p.selected);
                },
                b -> new Request(b.readUtf(36), b.readUtf(36), b.readVarLong(), b.readEnum(Operation.class),
                        b.readUtf(64), b.readBoolean()));
        @Override public Type<Request> type() { return TYPE; }
    }
    public record Response(String token, Status status, String json, String message) implements CustomPacketPayload {
        public Response {
            DiplomacyVolunteerView.token(token);
            if (status == null) throw new IllegalArgumentException("Missing volunteer response status");
            CountryView.text(json, MAX_JSON);
            CountryView.text(message, 1000);
            if ((status == Status.OPEN || status == Status.UPDATED) == json.isEmpty())
                throw new IllegalArgumentException("Invalid volunteer response state");
        }
        public static final Type<Response> TYPE = new Type<>(Identifier.fromNamespaceAndPath("hoi", "diplomacy_volunteer_response_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Response> CODEC = StreamCodec.of(
                (b, p) -> {
                    b.writeUtf(p.token, 36);
                    b.writeEnum(p.status);
                    b.writeUtf(p.json, MAX_JSON);
                    b.writeUtf(p.message, 1000);
                },
                b -> new Response(b.readUtf(36), b.readEnum(Status.class), b.readUtf(MAX_JSON), b.readUtf(1000)));
        public static Response of(String token, Status status, DiplomacyVolunteerView view) {
            return new Response(token, status, JSON.toJson(Objects.requireNonNull(view)), "");
        }
        public static Response closed(String token, Status status, String message) {
            return new Response(token, status, "", message);
        }
        public DiplomacyVolunteerView view() {
            try { return ProtocolJsonCodec.read(JSON, json, DiplomacyVolunteerView.class); }
            catch (RuntimeException invalid) { throw new IllegalArgumentException("Invalid volunteer response", invalid); }
        }
        @Override public Type<Response> type() { return TYPE; }
    }
}
