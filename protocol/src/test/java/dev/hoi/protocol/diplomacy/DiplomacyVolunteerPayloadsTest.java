package dev.hoi.protocol.diplomacy;

import com.google.gson.Gson;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import java.util.List;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DiplomacyVolunteerPayloadsTest {
    private static final String TOKEN = "00000000-0000-0000-0000-000000000001";
    private static final String SESSION = "00000000-0000-0000-0000-000000000002";
    @Test void everyOperationRoundTripsWithOnlyItsPermittedSelectionFields() {
        for (var operation : DiplomacyVolunteerPayloads.Operation.values()) {
            var request = request(operation, operation == DiplomacyVolunteerPayloads.Operation.SELECT ? "a".repeat(64) : "",
                    operation == DiplomacyVolunteerPayloads.Operation.SELECT || operation == DiplomacyVolunteerPayloads.Operation.AIR);
            var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
            try {
                DiplomacyVolunteerPayloads.Request.CODEC.encode(buffer, request);
                assertEquals(request, DiplomacyVolunteerPayloads.Request.CODEC.decode(buffer));
                assertEquals(0, buffer.readableBytes());
            } finally { buffer.release(); }
        }
        assertFalse(request(DiplomacyVolunteerPayloads.Operation.SELECT, "choice", false).selected());
        assertFalse(request(DiplomacyVolunteerPayloads.Operation.AIR, "", false).selected());
    }
    @Test void forgedEnvelopeAndOperationArgumentsAreRejected() {
        var select = DiplomacyVolunteerPayloads.Operation.SELECT;
        assertThrows(IllegalArgumentException.class, () -> request(select, "", true));
        assertThrows(IllegalArgumentException.class, () -> request(select, "x".repeat(65), true));
        assertThrows(IllegalArgumentException.class, () -> request(null, "", false));
        assertThrows(IllegalArgumentException.class, () -> new DiplomacyVolunteerPayloads.Request("not-a-token", SESSION, 1, select, "choice", true));
        assertThrows(IllegalArgumentException.class, () -> new DiplomacyVolunteerPayloads.Request(TOKEN, "not-a-session", 1, select, "choice", true));
        assertThrows(IllegalArgumentException.class, () -> new DiplomacyVolunteerPayloads.Request(TOKEN, SESSION, -1, select, "choice", true));
        for (var operation : DiplomacyVolunteerPayloads.Operation.values()) {
            if (operation != select) assertThrows(IllegalArgumentException.class, () -> request(operation, "injected", false));
            if (operation != select && operation != DiplomacyVolunteerPayloads.Operation.AIR)
                assertThrows(IllegalArgumentException.class, () -> request(operation, "", true));
        }
    }
    @Test void oversizedWireKeyIsRejectedByTheDecoderBeforeTheHandler() {
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            buffer.writeUtf(TOKEN);
            buffer.writeUtf(SESSION);
            buffer.writeVarLong(9);
            buffer.writeEnum(DiplomacyVolunteerPayloads.Operation.SELECT);
            buffer.writeUtf("x".repeat(65));
            buffer.writeBoolean(true);
            assertThrows(DecoderException.class, () -> DiplomacyVolunteerPayloads.Request.CODEC.decode(buffer));
        } finally { buffer.release(); }
    }
    @Test void authoritativeResponsesAndRevocationsPreserveWireState() {
        var view = new DiplomacyVolunteerView(SESSION, 9, "KOR", "PRK", "의용군 파병", "원문\n\n§H설명§!\n", "7일", "",
                List.of(), List.of(), "선택 필요", "", "", false, false, "", false, "군을 선택하십시오.");
        for (var status : DiplomacyVolunteerPayloads.Status.values()) {
            boolean open = status == DiplomacyVolunteerPayloads.Status.OPEN || status == DiplomacyVolunteerPayloads.Status.UPDATED;
            var response = open ? DiplomacyVolunteerPayloads.Response.of(TOKEN, status, view)
                    : DiplomacyVolunteerPayloads.Response.closed(TOKEN, status, "서버 응답\n\n보존");
            var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
            try {
                DiplomacyVolunteerPayloads.Response.CODEC.encode(buffer, response);
                var decoded = DiplomacyVolunteerPayloads.Response.CODEC.decode(buffer);
                assertEquals(response, decoded);
                assertEquals(0, buffer.readableBytes());
                if (open) assertEquals(view, decoded.view());
                else assertThrows(IllegalArgumentException.class, decoded::view);
            } finally { buffer.release(); }
        }
    }
    @Test void malformedAndOversizedResponsesCannotCreateAnAuthoritativeView() {
        var open = DiplomacyVolunteerPayloads.Status.OPEN;
        var revoked = DiplomacyVolunteerPayloads.Status.REVOKED;
        assertThrows(IllegalArgumentException.class, () -> new DiplomacyVolunteerPayloads.Response(TOKEN, open, "", ""));
        assertThrows(IllegalArgumentException.class, () -> new DiplomacyVolunteerPayloads.Response(TOKEN, revoked, "{}", ""));
        assertThrows(IllegalArgumentException.class, () -> new DiplomacyVolunteerPayloads.Response(TOKEN, open, "x".repeat(DiplomacyVolunteerPayloads.MAX_JSON + 1), ""));
        assertThrows(IllegalArgumentException.class, () -> new DiplomacyVolunteerPayloads.Response(TOKEN, revoked, "", "x".repeat(1001)));
        for (var json : List.of("null", "[]", "[", "\"invalid\"", "{}")) {
            assertThrows(IllegalArgumentException.class, () -> new DiplomacyVolunteerPayloads.Response(TOKEN, open, json, "").view());
        }
        var valid = new DiplomacyVolunteerView(SESSION, 1, "KOR", "PRK", "", "", "", "", List.of(), List.of(),
                "", "", "", false, false, "", false, "");
        var json = new Gson().toJsonTree(valid).getAsJsonObject();
        json.add("selectedArmyKeys", new Gson().toJsonTree(List.of("not-issued")));
        assertThrows(IllegalArgumentException.class, () -> new DiplomacyVolunteerPayloads.Response(TOKEN, open, json.toString(), "").view());
    }
    private static DiplomacyVolunteerPayloads.Request request(DiplomacyVolunteerPayloads.Operation operation,
            String key, boolean selected) {
        return new DiplomacyVolunteerPayloads.Request(TOKEN, SESSION, 9, operation, key, selected);
    }
}
