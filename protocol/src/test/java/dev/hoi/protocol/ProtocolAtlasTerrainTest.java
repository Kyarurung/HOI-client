package dev.hoi.protocol;

import io.netty.buffer.Unpooled;
import java.util.*;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProtocolAtlasTerrainTest {
    @Test void pagesRoundTripWithoutSharingMutableLists() {
        var strips = new ArrayList<>(List.of(new ProtocolAtlasTerrain.Strip(16, 5, 16, 65, 68, 1234)));
        var page = new ProtocolAtlasTerrain.Page(UUID.randomUUID(), "hoi:atlas_army_test", 0, 1, strips);
        strips.clear();
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            ProtocolAtlasTerrain.Page.CODEC.encode(buffer, page);
            assertEquals(page, ProtocolAtlasTerrain.Page.CODEC.decode(buffer));
            assertEquals(1, page.strips().size()); assertEquals(0, buffer.readableBytes());
        } finally { buffer.release(); }
    }
    @Test void rejectsOrdinaryWorldsAndUnboundedGeometry() {
        assertThrows(IllegalArgumentException.class, () -> new ProtocolAtlasTerrain.Page(UUID.randomUUID(), "minecraft:overworld", 0, 1, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new ProtocolAtlasTerrain.Page(UUID.randomUUID(), "hoi:atlas_test", 0, 257, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new ProtocolAtlasTerrain.Strip(15, 0, 2, 65, 66, 1));
        assertThrows(IllegalArgumentException.class, () -> new ProtocolAtlasTerrain.Strip(0, 0, 1, 65, 65, 1));
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            buffer.writeUUID(UUID.randomUUID()); buffer.writeUtf("hoi:atlas_test"); buffer.writeVarInt(0);
            buffer.writeVarInt(1); buffer.writeVarInt(1025);
            assertThrows(RuntimeException.class, () -> ProtocolAtlasTerrain.Page.CODEC.decode(buffer));
        } finally { buffer.release(); }
    }
}
