package dev.hoi.protocol;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import java.util.*;

public final class UnitAudioProtocol {
    public static final int RANGE = 7, MAX_VOICES = 3;
    private static boolean registered;
    private UnitAudioProtocol() {}
    public static synchronized void registerPayloadTypes() {
        if (registered) return;
        PayloadTypeRegistry.clientboundPlay().register(Snapshot.TYPE, Snapshot.CODEC);
        registered = true;
    }
    public enum Sound {
        FOOT, TRUCK, APC, TANK, LIGHT_TANK, RIFLE, MACHINE_GUN, CANNON, AUTOCANNON;
        public String event() { return "unit." + name().toLowerCase(Locale.ROOT); }
        public boolean combat() { return ordinal() >= RIFLE.ordinal(); }
    }
    public record Emitter(String id, Sound sound, double x, double y, double z) {
        public Emitter {
            if (id == null || id.isBlank() || id.length() > 128) throw new IllegalArgumentException("Invalid unit audio ID");
            Objects.requireNonNull(sound);
            for (double value : new double[]{x,y,z}) if (!Double.isFinite(value) || Math.abs(value) > 30_000_000)
                throw new IllegalArgumentException("Invalid unit audio position");
        }
        public double distanceSquared(double px, double py, double pz) {
            return (x-px)*(x-px)+(y-py)*(y-py)+(z-pz)*(z-pz);
        }
    }
    public record Snapshot(String dimension, List<Emitter> emitters) implements CustomPacketPayload {
        public Snapshot {
            if (dimension == null || dimension.length() > 128 || Identifier.tryParse(dimension) == null)
                throw new IllegalArgumentException("Invalid unit audio dimension");
            emitters = List.copyOf(emitters);
            if (emitters.size() > MAX_VOICES || emitters.stream().map(Emitter::id).distinct().count() != emitters.size())
                throw new IllegalArgumentException("Invalid unit audio voices");
        }
        public static final Type<Snapshot> TYPE = new Type<>(Identifier.fromNamespaceAndPath("hoi", "unit_audio_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Snapshot> CODEC = StreamCodec.of((b,p) -> {
            b.writeUtf(p.dimension,128); b.writeByte(p.emitters.size());
            for (var e : p.emitters) { b.writeUtf(e.id,128); b.writeEnum(e.sound); b.writeDouble(e.x); b.writeDouble(e.y); b.writeDouble(e.z); }
        }, b -> {
            String dimension = b.readUtf(128); int count = b.readUnsignedByte();
            if (count > MAX_VOICES) throw new IllegalArgumentException("Too many unit sounds");
            var emitters = new ArrayList<Emitter>(count);
            for (int i=0;i<count;i++) emitters.add(new Emitter(b.readUtf(128),b.readEnum(Sound.class),b.readDouble(),b.readDouble(),b.readDouble()));
            return new Snapshot(dimension,emitters);
        });
        @Override public Type<Snapshot> type() { return TYPE; }
    }
}
