package dev.hoi.protocol;

import java.util.*;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public final class ProtocolAtlasTerrain {
    public static final int PAGE_SIZE = 1024;
    public static final int MAX_PAGES = 256;
    public record Strip(int x, int z, int width, int bottom, int top, int state) {
        public Strip {
            if (x < 0 || z < 0 || x >= 4096 || z >= 4096 || width < 1 || width > 16
                    || x / 16 != (x + width - 1) / 16 || bottom < -64 || top > 512 || top <= bottom
                    || top - bottom > 16 || state < 0 || state > 1_000_000)
                throw new IllegalArgumentException("Invalid atlas terrain strip");
        }
    }
    public record Page(UUID revision, String dimension, int index, int count, List<Strip> strips) implements CustomPacketPayload {
        public static final Type<Page> TYPE = new Type<>(Identifier.parse("hoi:atlas_terrain_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Page> CODEC = StreamCodec.of((b, p) -> {
            b.writeUUID(p.revision); b.writeUtf(p.dimension, 160); b.writeVarInt(p.index); b.writeVarInt(p.count);
            b.writeVarInt(p.strips.size());
            for (var s : p.strips) {
                b.writeVarInt(s.x); b.writeVarInt(s.z); b.writeVarInt(s.width);
                b.writeInt(s.bottom); b.writeInt(s.top); b.writeVarInt(s.state);
            }
        }, b -> {
            UUID revision = b.readUUID(); String dimension = b.readUtf(160);
            int index = b.readVarInt(), count = b.readVarInt(), size = b.readVarInt();
            validate(dimension, index, count, size);
            var strips = new ArrayList<Strip>(size);
            for (int i = 0; i < size; i++) strips.add(new Strip(b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readInt(), b.readInt(), b.readVarInt()));
            return new Page(revision, dimension, index, count, strips);
        });
        public Page {
            Objects.requireNonNull(revision); validate(dimension, index, count, strips.size()); strips = List.copyOf(strips);
        }
        @Override public Type<Page> type() { return TYPE; }
    }
    private static void validate(String dimension, int index, int count, int size) {
        var id = Identifier.parse(dimension);
        if (!id.getNamespace().equals("hoi") || !id.getPath().startsWith("atlas_") || dimension.length() > 160
                || count < 1 || count > MAX_PAGES || index < 0 || index >= count || size < 0 || size > PAGE_SIZE)
            throw new IllegalArgumentException("Invalid atlas terrain page");
    }
    private static boolean registered;
    public static synchronized void registerPayloadTypes() {
        if (registered) return;
        PayloadTypeRegistry.clientboundPlay().register(Page.TYPE, Page.CODEC); registered = true;
    }
    private ProtocolAtlasTerrain() {}
}
