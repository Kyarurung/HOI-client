package dev.hoi.protocol.doctrine;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DoctrinePayloadsTest {
    private static DoctrineView view(List<DoctrineView.Folder> folders) {
        return new DoctrineView(UUID.randomUUID().toString(), 1, "KOR", 0, "land", "ARMY", "300", "", folders, List.of(), List.of(), "");
    }
    @Test void boundedPrivateViewRoundTripsAndRejectsMalformedWireValues() {
        var original = view(List.of(new DoctrineView.Folder("land", "육군 교리", "doctrine/gfx_landdoctrine_tab_large", 0)));
        assertEquals(original, DoctrinePayloads.Response.of(original).view());
        assertThrows(IllegalArgumentException.class, () -> new DoctrinePayloads.Response("x".repeat(DoctrinePayloads.MAX_JSON + 1)));
        assertThrows(IllegalArgumentException.class, () -> new DoctrinePayloads.Response("{\"revision\":-1}").view());
        assertThrows(IllegalArgumentException.class, () -> new DoctrinePayloads.Request(DoctrinePayloads.Kind.OPEN, "x".repeat(37), 0, "land", "", ""));
        assertThrows(IllegalArgumentException.class, () -> new DoctrinePayloads.Request(DoctrinePayloads.Kind.SELECT_SUBDOCTRINE, "", -1, "land", "", ""));
        assertThrows(IllegalArgumentException.class, () -> new DoctrinePayloads.Request(DoctrinePayloads.Kind.OPEN, UUID.randomUUID().toString(), 0, "land", "", "", "prk"));
        assertEquals("", new DoctrinePayloads.Request(DoctrinePayloads.Kind.OPEN, UUID.randomUUID().toString(), 0, "land", "", "").target());
        var intel = new DoctrinePayloads.Request(DoctrinePayloads.Kind.OPEN, UUID.randomUUID().toString(), 0, "land", "", "", "PRK");
        var buffer = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), net.minecraft.core.RegistryAccess.EMPTY);
        DoctrinePayloads.Request.CODEC.encode(buffer, intel);
        assertEquals(intel, DoctrinePayloads.Request.CODEC.decode(buffer));
    }
    @Test void sourceLabelsAndAdvancedDetailsRemainBoundedAndRoundTrip() {
        var base = view(List.of());
        var choice = new DoctrineView.Choice("motorized_infantry", "차량화보병", "", "", "100", "즉시 제공", true, false, "£doctrine_mastery §H150§!");
        var original = new DoctrineView(base.session(), 1, "KOR", 0, "land", "ARMY", "300", "", List.of(), List.of(choice), List.of(), "", Map.of("SUB_DOCTRINE_SELECTION_TITLE", "하위 교리 선택"));
        assertEquals(original, DoctrinePayloads.Response.of(original).view());
        assertThrows(IllegalArgumentException.class, () -> new DoctrineView.Choice("x", "", "", "", "", "", false, false, "x".repeat(24001)));
        var labels = new HashMap<String, String>(); for (int i = 0; i < 33; i++) labels.put("k" + i, "v");
        assertThrows(IllegalArgumentException.class, () -> new DoctrineView("", 0, "KOR", 0, "land", "ARMY", "0", "", List.of(), List.of(), List.of(), "", labels));
        assertThrows(IllegalArgumentException.class, () -> new DoctrineView("", 0, "KOR", 0, "land", "ARMY", "0", "", List.of(), List.of(), List.of(), "", Map.of("key", "x".repeat(8001))));
    }
    @Test void duplicateIdsAndExcessiveTracksAreRejectedBeforeTransmission() {
        var folder = new DoctrineView.Folder("land", "육군 교리", "", 0);
        assertThrows(IllegalArgumentException.class, () -> view(List.of(folder, folder)));
        assertThrows(IllegalArgumentException.class, () -> new DoctrineView.Folder("land", "", "", 4));
        var tracks = new ArrayList<DoctrineView.Track>();
        for (int i = 0; i < 65; i++) tracks.add(new DoctrineView.Track("t" + i, "", "", "", "", "", "", "—", "—", "—", List.of(), List.of()));
        assertThrows(IllegalArgumentException.class, () -> new DoctrineView("", 0, "KOR", 0, "land", "ARMY", "0", "", List.of(), List.of(), tracks, ""));
    }
}
