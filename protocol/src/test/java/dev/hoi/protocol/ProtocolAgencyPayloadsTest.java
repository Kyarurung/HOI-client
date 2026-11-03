package dev.hoi.protocol;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import java.util.List;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProtocolAgencyPayloadsTest {
    @Test void operationDetailIsOptionalBoundedAndPreserved() {
        var gson = new com.google.gson.Gson();
        var legacy = new ProtocolAgencyView.Operation("op", "cancel:op", "source", "작전", "PRK", 1, 0, 0, "2020년 1월 2일", 1, 2, List.of());
        var json = gson.toJsonTree(legacy).getAsJsonObject();
        json.remove("detail");
        assertEquals("", gson.fromJson(json, ProtocolAgencyView.Operation.class).detail());
        String exact = "§Y작전§!\n작전이 §H2020년 1월 2일§!에 완료될 것입니다\n\n세부사항을 보려면 §G클릭§!.";
        json.addProperty("detail", exact);
        var operation = gson.fromJson(json, ProtocolAgencyView.Operation.class);
        var view = new ProtocolAgencyView("session", 4, "KOR", "기관", "", List.of(), "", List.of(), List.of(), List.of(), List.of(operation), List.of());
        assertEquals(exact, ProtocolAgencyPayloads.Response.of(view).view().operations().getFirst().detail());
        json.addProperty("detail", "가".repeat(1000));
        assertEquals(1000, gson.fromJson(json, ProtocolAgencyView.Operation.class).detail().length());
        assertThrows(IllegalArgumentException.class, () -> new ProtocolAgencyView.Operation("op", "cancel:op", "source", "작전", "PRK", 1, 0, 0, "", 1, 2, List.of(), 1, 0, "", "", "", List.of(), "가".repeat(1001)));
    }
    @Test void phaseProgressIsOptionalBoundedAndServerSupplied() {
        var phase = new ProtocolAgencyView.Phase("phase", "name", "", "", "picture", "icon");
        var operation = new ProtocolAgencyView.Operation("op", "cancel:op", "source", "name", "PRK", 1, 0, 0, "", 23, 90,
                List.of("one"), 1, 35, "", "", "", List.of(phase), "", new ProtocolAgencyView.Progress(0, 69, 90));
        var view = new ProtocolAgencyView("session", 1, "KOR", "", "", List.of(), "", List.of(), List.of(), List.of(), List.of(operation), List.of());
        assertEquals(view, ProtocolAgencyPayloads.Response.of(view).view());
        assertEquals(69.0 / 90, operation.progress().fraction());
        var gson = new com.google.gson.Gson();
        var legacy = gson.toJsonTree(operation).getAsJsonObject();
        legacy.remove("progress");
        assertNull(gson.fromJson(legacy, ProtocolAgencyView.Operation.class).progress());
        assertThrows(IllegalArgumentException.class, () -> new ProtocolAgencyView.Progress(-1, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> new ProtocolAgencyView.Progress(0, 2, 1));
        assertThrows(IllegalArgumentException.class, () -> new ProtocolAgencyView.Progress(0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new ProtocolAgencyView.Operation("op", "cancel:op", "source", "name", "PRK", 1, 0, 0, "", 23, 90,
                List.of(), 1, 35, "", "", "", List.of(phase), "", new ProtocolAgencyView.Progress(1, 0, 90)));
    }
    @Test void upgradeCostAndExactTooltipSurviveResponse() {
        String detail = "£defense_texticon 방첩 활동: §a+1.50§r\n\n§#9AABA8원문 설명§r";
        var item = new ProtocolAgencyView.Item("upgrade:DOMESTIC_INTELLIGENCE", "upgrades", "국내 정보", "1 / 4",
                detail, "agency/domestic_intelligence", "개선", true, .25, List.of(), 5);
        var view = new ProtocolAgencyView("session", 3, "KOR", "국가정보원", "", List.of(item), "");
        assertEquals(view, ProtocolAgencyPayloads.Response.of(view).view());
        assertThrows(IllegalArgumentException.class, () -> new ProtocolAgencyView.Item("id", "upgrades", "", "", "", "", "", false, 0, List.of(), -1));
        assertThrows(IllegalArgumentException.class, () -> new ProtocolAgencyView.Item("id", "upgrades", "", "", "", "", "", false, 0, List.of(), 1001));
    }
    @Test void argumentsRoundTripAtTheLimit() {
        var request = new ProtocolAgencyPayloads.Request(ProtocolAgencyPayloads.Kind.CALL, "session", 7,
                "choice", List.of("가".repeat(128), "둘", "셋", "넷"));
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            ProtocolAgencyPayloads.Request.CODEC.encode(buffer, request);
            assertEquals(request, ProtocolAgencyPayloads.Request.CODEC.decode(buffer));
            assertEquals(0, buffer.readableBytes());
        } finally { buffer.release(); }
    }
    @Test void rejectsOversizedArgumentCountBeforeReadingElements() {
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            buffer.writeEnum(ProtocolAgencyPayloads.Kind.CALL);
            buffer.writeUtf("session");
            buffer.writeVarLong(7);
            buffer.writeUtf("choice");
            buffer.writeVarInt(5);
            assertThrows(DecoderException.class, () -> ProtocolAgencyPayloads.Request.CODEC.decode(buffer));
        } finally { buffer.release(); }
    }
}
