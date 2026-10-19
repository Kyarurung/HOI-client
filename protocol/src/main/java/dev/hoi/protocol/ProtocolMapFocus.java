package dev.hoi.protocol;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public final class ProtocolMapFocus {
    private static boolean registered;
    private ProtocolMapFocus() {}
    public static synchronized void registerPayloadTypes() {
        if (registered) return;
        PayloadTypeRegistry.clientboundPlay().register(State.TYPE, State.CODEC);
        registered = true;
    }
    public record State(int state, int x, int z, int minX, int minZ, int maxX, int maxZ) implements CustomPacketPayload {
        public State {
            if (state < 1 || state > 999_999) throw new IllegalArgumentException("Invalid map focus state");
            if (minX < 0 || minZ < 0 || maxX > 65_535 || maxZ > 65_535 || minX > x || x > maxX || minZ > z || z > maxZ)
                throw new IllegalArgumentException("Invalid map focus area");
        }
        public static final Type<State> TYPE = new Type<>(Identifier.fromNamespaceAndPath("hoi", "story_map_focus_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf, State> CODEC = StreamCodec.of((b, p) -> {
            b.writeVarInt(p.state()); b.writeVarInt(p.x()); b.writeVarInt(p.z());
            b.writeVarInt(p.minX()); b.writeVarInt(p.minZ()); b.writeVarInt(p.maxX()); b.writeVarInt(p.maxZ());
        }, b -> new State(b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readVarInt()));
        @Override public Type<State> type() { return TYPE; }
    }
}
