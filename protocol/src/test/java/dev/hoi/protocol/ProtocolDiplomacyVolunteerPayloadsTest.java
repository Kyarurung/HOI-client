package dev.hoi.protocol;

import com.google.gson.Gson;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import java.util.List;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProtocolDiplomacyVolunteerPayloadsTest {
    private static final String TOKEN = "00000000-0000-0000-0000-000000000001";
    private static final String SESSION = "00000000-0000-0000-0000-000000000002";
    @Test void everyOperationRoundTripsWithOnlyItsPermittedSelectionFields() {
        for (var operation : ProtocolDiplomacyVolunteerPayloads.Operation.values()) {
            var request = request(operation, operation == ProtocolDiplomacyVolunteerPayloads.Operation.SELECT ? "a".repeat(64) : "",
                    operation == ProtocolDiplomacyVolunteerPayloads.Operation.SELECT || operation == ProtocolDiplomacyVolunteerPayloads.Operation.AIR);
            var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
            try {
                ProtocolDiplomacyVolunteerPayloads.Request.CODEC.encode(buffer, request);
                assertEquals(request, ProtocolDiplomacyVolunteerPayloads.Request.CODEC.decode(buffer));
                assertEquals(0, buffer.readableBytes());
            } finally { buffer.release(); }
        }
        assertFalse(request(ProtocolDiplomacyVolunteerPayloads.Operation.SELECT, "choice", false).selected());
        assertFalse(request(ProtocolDiplomacyVolunteerPayloads.Operation.AIR, "", false).selected());
    }
    @Test void forgedEnvelopeAndOperationArgumentsAreRejected() {
        var select = ProtocolDiplomacyVolunteerPayloads.Operation.SELECT;
        assertThrows(IllegalArgumentException.class, () -> request(select, "", true));
        assertThrows(IllegalArgumentException.class, () -> request(select, "x".repeat(65), true));
        assertThrows(IllegalArgumentException.class, () -> request(null, "", false));
        assertThrows(IllegalArgumentException.class, () -> new ProtocolDiplomacyVolunteerPayloads.Request("not-a-token", SESSION, 1, select, "choice", true));
        assertThrows(IllegalArgumentException.class, () -> new ProtocolDiplomacyVolunteerPayloads.Request(TOKEN, "not-a-session", 1, select, "choice", true));
        assertThrows(IllegalArgumentException.class, () -> new ProtocolDiplomacyVolunteerPayloads.Request(TOKEN, SESSION, -1, select, "choice", true));
        for (var operation : ProtocolDiplomacyVolunteerPayloads.Operation.values()) {
            if (operation != select) assertThrows(IllegalArgumentException.class, () -> request(operation, "injected", false));
            if (operation != select && operation != ProtocolDiplomacyVolunteerPayloads.Operation.AIR)
                assertThrows(IllegalArgumentException.class, () -> request(operation, "", true));
        }
    }
    @Test void oversizedWireKeyIsRejectedByTheDecoderBeforeTheHandler() {
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            buffer.writeUtf(TOKEN);
            buffer.writeUtf(SESSION);
            buffer.writeVarLong(9);
            buffer.writeEnum(ProtocolDiplomacyVolunteerPayloads.Operation.SELECT);
            buffer.writeUtf("x".repeat(65));
            buffer.writeBoolean(true);
            assertThrows(DecoderException.class, () -> ProtocolDiplomacyVolunteerPayloads.Request.CODEC.decode(buffer));
        } finally { buffer.release(); }
    }
    @Test void authoritativeResponsesAndRevocationsPreserveWireState() {
        var view = new ProtocolDiplomacyVolunteerView(SESSION, 9, "KOR", "PRK", "의용군 파병", "원문\n\n§H설명§!\n", "7일", "",
                List.of(), List.of(), "선택 필요", "", "", false, false, "", false, "군을 선택하십시오.");
        for (var status : ProtocolDiplomacyVolunteerPayloads.Status.values()) {
            boolean open = status == ProtocolDiplomacyVolunteerPayloads.Status.OPEN || status == ProtocolDiplomacyVolunteerPayloads.Status.UPDATED;
            var response = open ? ProtocolDiplomacyVolunteerPayloads.Response.of(TOKEN, status, view)
                    : ProtocolDiplomacyVolunteerPayloads.Response.closed(TOKEN, status, "서버 응답\n\n보존");
            var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
            try {
                ProtocolDiplomacyVolunteerPayloads.Response.CODEC.encode(buffer, response);
                var decoded = ProtocolDiplomacyVolunteerPayloads.Response.CODEC.decode(buffer);
                assertEquals(response, decoded);
                assertEquals(0, buffer.readableBytes());
                if (open) assertEquals(view, decoded.view());
                else assertThrows(IllegalArgumentException.class, decoded::view);
            } finally { buffer.release(); }
        }
    }
    @Test void malformedAndOversizedResponsesCannotCreateAnAuthoritativeView() {
        var open = ProtocolDiplomacyVolunteerPayloads.Status.OPEN;
        var revoked = ProtocolDiplomacyVolunteerPayloads.Status.REVOKED;
        assertThrows(IllegalArgumentException.class, () -> new ProtocolDiplomacyVolunteerPayloads.Response(TOKEN, open, "", ""));
        assertThrows(IllegalArgumentException.class, () -> new ProtocolDiplomacyVolunteerPayloads.Response(TOKEN, revoked, "{}", ""));
        assertThrows(IllegalArgumentException.class, () -> new ProtocolDiplomacyVolunteerPayloads.Response(TOKEN, open, "x".repeat(ProtocolDiplomacyVolunteerPayloads.MAX_JSON + 1), ""));
        assertThrows(IllegalArgumentException.class, () -> new ProtocolDiplomacyVolunteerPayloads.Response(TOKEN, revoked, "", "x".repeat(1001)));
        for (var json : List.of("null", "[]", "[", "\"invalid\"", "{}")) {
            assertThrows(IllegalArgumentException.class, () -> new ProtocolDiplomacyVolunteerPayloads.Response(TOKEN, open, json, "").view());
        }
        var valid = new ProtocolDiplomacyVolunteerView(SESSION, 1, "KOR", "PRK", "", "", "", "", List.of(), List.of(),
                "", "", "", false, false, "", false, "");
        var json = new Gson().toJsonTree(valid).getAsJsonObject();
        json.add("selectedArmyKeys", new Gson().toJsonTree(List.of("not-issued")));
        assertThrows(IllegalArgumentException.class, () -> new ProtocolDiplomacyVolunteerPayloads.Response(TOKEN, open, json.toString(), "").view());
    }
    private static ProtocolDiplomacyVolunteerPayloads.Request request(ProtocolDiplomacyVolunteerPayloads.Operation operation,
            String key, boolean selected) {
        return new ProtocolDiplomacyVolunteerPayloads.Request(TOKEN, SESSION, 9, operation, key, selected);
    }
}
