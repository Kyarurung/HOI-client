package dev.hoi.protocol.map;

import dev.hoi.protocol.dialog.DialogPayloads;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MapSupplyHoverTest {
    private static net.minecraft.network.RegistryFriendlyByteBuf buffer() {
        return new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), net.minecraft.core.RegistryAccess.EMPTY);
    }
    @Test void hoverRequestsAndTooltipsRoundTripWithinBounds() {
        var request = new MapSupplyHover.Request(7, 0, -1, 0);
        var wire = buffer(); MapSupplyHover.Request.CODEC.encode(wire, request);
        assertEquals(request, MapSupplyHover.Request.CODEC.decode(wire));
        assertThrows(IllegalArgumentException.class, () -> new MapSupplyHover.Request(-1, 0, -1, 0));
        assertThrows(IllegalArgumentException.class, () -> new MapSupplyHover.Request(1, 0, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> new MapSupplyHover.Request(1, 0, -2, 0));
        var tooltip = new MapSupplyHover.Tooltip(7, "hub/1177", "§e제주§r의 §e보급 중심지§r:", "§e보급 중심지§r는");
        wire = buffer(); MapSupplyHover.Tooltip.CODEC.encode(wire, tooltip);
        assertEquals(tooltip, MapSupplyHover.Tooltip.CODEC.decode(wire));
        assertEquals("", new MapSupplyHover.Tooltip(1, null, null, null).delayed());
        assertThrows(IllegalArgumentException.class, () -> new MapSupplyHover.Tooltip(1, "", "x".repeat(4001), ""));
    }
    @Test void mapClicksCarryOnlyShiftAndControl() {
        var click = new DialogPayloads.MapClick(0, -1, 0, DialogPayloads.MapClick.SHIFT | DialogPayloads.MapClick.CONTROL);
        var wire = buffer(); DialogPayloads.MapClick.CODEC.encode(wire, click);
        assertEquals(click, DialogPayloads.MapClick.CODEC.decode(wire));
        assertEquals(0, new DialogPayloads.MapClick(0, -1, 0).modifiers());
        assertEquals(0, DialogPayloads.MapClick.INSTANCE.modifiers());
        assertThrows(IllegalArgumentException.class, () -> new DialogPayloads.MapClick(0, -1, 0, 4));
    }
}
