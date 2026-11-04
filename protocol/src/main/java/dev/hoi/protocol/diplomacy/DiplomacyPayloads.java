package dev.hoi.protocol.diplomacy;

import dev.hoi.protocol.ProtocolJsonCodec;
import dev.hoi.protocol.country.CountryView;
import com.google.gson.Gson;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public final class DiplomacyPayloads {
    private static final Gson JSON = new Gson();
    private static boolean registered;
    private DiplomacyPayloads() {}
    public enum Operation { PREVIEW, SUBMIT, CLOSE }
    public enum Status { OPEN, UPDATED, COMPLETED, REVOKED }
    public static synchronized void registerPayloadTypes() {
        if (registered) return;
        PayloadTypeRegistry.serverboundPlay().register(Request.TYPE, Request.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Response.TYPE, Response.CODEC);
        registered = true;
    }
    private static void token(String token) { CountryView.text(token, 36); UUID.fromString(token); }
    public record Request(String token, String session, long revision, Operation operation,
            List<DiplomacyView.LeaseLine> lines) implements CustomPacketPayload {
        public Request {
            DiplomacyPayloads.token(token);
            DiplomacyPayloads.token(session);
            if (revision < 0) throw new IllegalArgumentException("Invalid diplomacy revision");
            Objects.requireNonNull(operation);
            lines = DiplomacyView.leaseLines(lines);
            if (operation == Operation.CLOSE && !lines.isEmpty()) throw new IllegalArgumentException("Unexpected closed lease draft");
        }
        public static final Type<Request> TYPE = new Type<>(Identifier.fromNamespaceAndPath("hoi", "diplomacy_lease_request_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Request> CODEC = StreamCodec.of(
                (b, p) -> b.writeUtf(JSON.toJson(p), 12_000),
                b -> ProtocolJsonCodec.read(JSON, b.readUtf(12_000), Request.class));
        @Override public Type<Request> type() { return TYPE; }
    }
    public record Response(String token, Status status, String json, String message) implements CustomPacketPayload {
        public Response {
            DiplomacyPayloads.token(token);
            Objects.requireNonNull(status);
            CountryView.text(json, 200_000);
            CountryView.text(message, 1000);
            if ((status == Status.OPEN || status == Status.UPDATED) == json.isEmpty())
                throw new IllegalArgumentException("Invalid diplomacy response state");
        }
        public static final Type<Response> TYPE = new Type<>(Identifier.fromNamespaceAndPath("hoi", "diplomacy_lease_response_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Response> CODEC = StreamCodec.of(
                (b, p) -> { b.writeUtf(p.token, 36); b.writeEnum(p.status); b.writeUtf(p.json, 200_000); b.writeUtf(p.message, 1000); },
                b -> new Response(b.readUtf(36), b.readEnum(Status.class), b.readUtf(200_000), b.readUtf(1000)));
        public static Response of(String token, Status status, DiplomacyView view) { return new Response(token, status, JSON.toJson(view), ""); }
        public static Response closed(String token, Status status, String message) { return new Response(token, status, "", message); }
        public DiplomacyView view() {
            try { return ProtocolJsonCodec.read(JSON, json, DiplomacyView.class); }
            catch (RuntimeException invalid) { throw new IllegalArgumentException("Invalid diplomacy response", invalid); }
        }
        @Override public Type<Response> type() { return TYPE; }
    }
}
