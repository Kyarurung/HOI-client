package dev.hoi.protocol;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import java.util.*;


public final class AtlasSceneProtocol {
    public static final int PAGE_SIZE = 1024;
    public static final int MAX_PAGES = 256;
    public enum Material { BLACK, RED, WATER, FOREST, JUNGLE, MARSH, DESERT, CITY, NAVY_RIVER, AIR_RIVER, GRAY, DEPLOYMENT, DMZ }
    public record Box(float x, float y, float z, float sx, float sy, float sz, float yaw, Material material) {
        public Box {
            for (float coordinate : new float[]{x, y, z, yaw})
                if (!Float.isFinite(coordinate) || Math.abs(coordinate) > 65536) throw new IllegalArgumentException("Invalid atlas coordinate");
            for (float extent : new float[]{sx, sy, sz})
                if (!Float.isFinite(extent) || extent <= 0 || extent > 8192) throw new IllegalArgumentException("Invalid atlas extent");
            Objects.requireNonNull(material);
        }
        private void write(RegistryFriendlyByteBuf b) {
            b.writeFloat(x); b.writeFloat(y); b.writeFloat(z); b.writeFloat(sx); b.writeFloat(sy); b.writeFloat(sz); b.writeFloat(yaw); b.writeEnum(material);
        }
        private static Box read(RegistryFriendlyByteBuf b) {
            return new Box(b.readFloat(), b.readFloat(), b.readFloat(), b.readFloat(), b.readFloat(), b.readFloat(), b.readFloat(), b.readEnum(Material.class));
        }
    }
    public record Page(UUID scene, String dimension, int index, int count, List<Box> boxes) implements CustomPacketPayload {
        public static final Type<Page> TYPE = new Type<>(Identifier.parse("hoi:atlas_scene_v2"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Page> CODEC = StreamCodec.of((b, p) -> {
            b.writeUUID(p.scene); b.writeUtf(p.dimension, 160); b.writeVarInt(p.index); b.writeVarInt(p.count); b.writeVarInt(p.boxes.size());
            p.boxes.forEach(box -> box.write(b));
        }, b -> {
            UUID scene = b.readUUID(); String dimension = b.readUtf(160); int index = b.readVarInt(), count = b.readVarInt(), size = b.readVarInt();
            if (size < 0 || size > PAGE_SIZE || count < 1 || count > MAX_PAGES || index < 0 || index >= count)
                throw new IllegalArgumentException("Invalid atlas page");
            var boxes = new ArrayList<Box>(size);
            for (int i = 0; i < size; i++) boxes.add(Box.read(b));
            return new Page(scene, dimension, index, count, boxes);
        });
        public Page {
            Objects.requireNonNull(scene); Identifier.parse(dimension);
            if (dimension.length() > 160 || count < 1 || count > MAX_PAGES || index < 0 || index >= count || boxes.size() > PAGE_SIZE)
                throw new IllegalArgumentException("Invalid atlas page");
            boxes = List.copyOf(boxes);
        }
        @Override public Type<Page> type() { return TYPE; }
    }
    public record Ready(UUID scene) implements CustomPacketPayload {
        public static final Type<Ready> TYPE = new Type<>(Identifier.parse("hoi:atlas_scene_ready_v2"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Ready> CODEC = StreamCodec.of((b, p) -> b.writeUUID(p.scene), b -> new Ready(b.readUUID()));
        @Override public Type<Ready> type() { return TYPE; }
    }
    private static boolean registered;
    public static synchronized void registerPayloadTypes() {
        if (registered) return;
        PayloadTypeRegistry.clientboundPlay().register(Page.TYPE, Page.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(Ready.TYPE, Ready.CODEC);
        registered = true;
    }
    private AtlasSceneProtocol() {}
}
