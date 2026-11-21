package dev.hoi.protocol.map;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public final class MapSupplyHover {
    private static boolean registered;
    private MapSupplyHover() {}
    public static synchronized void registerPayloadTypes() {
        if (registered) return;
        PayloadTypeRegistry.serverboundPlay().register(Request.TYPE, Request.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Tooltip.TYPE, Tooltip.CODEC);
        registered = true;
    }
    public record Request(long sequence, double x, double y, double z) implements CustomPacketPayload {
        public Request {
            double length = x * x + y * y + z * z;
            if (sequence < 0 || !Double.isFinite(length) || Math.abs(length - 1) > .001 || y >= -.00001)
                throw new IllegalArgumentException("Invalid supply hover");
        }
        public static final Type<Request> TYPE = new Type<>(Identifier.fromNamespaceAndPath("hoi", "map_supply_hover_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Request> CODEC = StreamCodec.of((b, p) -> {
            b.writeVarLong(p.sequence()); b.writeDouble(p.x()); b.writeDouble(p.y()); b.writeDouble(p.z());
        }, b -> new Request(b.readVarLong(), b.readDouble(), b.readDouble(), b.readDouble()));
        @Override public Type<Request> type() { return TYPE; }
    }
    public record Tooltip(long sequence, String target, String text, String delayed) implements CustomPacketPayload {
        public Tooltip {
            target = target == null ? "" : target; text = text == null ? "" : text; delayed = delayed == null ? "" : delayed;
            if (sequence < 0 || target.length() > 80 || text.length() > 4000 || delayed.length() > 4000)
                throw new IllegalArgumentException("Invalid supply tooltip");
        }
        public static final Type<Tooltip> TYPE = new Type<>(Identifier.fromNamespaceAndPath("hoi", "map_supply_tooltip_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Tooltip> CODEC = StreamCodec.of((b, p) -> {
            b.writeVarLong(p.sequence()); b.writeUtf(p.target(), 80); b.writeUtf(p.text(), 4000); b.writeUtf(p.delayed(), 4000);
        }, b -> new Tooltip(b.readVarLong(), b.readUtf(80), b.readUtf(4000), b.readUtf(4000)));
        @Override public Type<Tooltip> type() { return TYPE; }
    }
}
