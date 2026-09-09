package dev.hoi.protocol;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import java.util.Objects;

/** Server-issued local audio only; no client request or arbitrary resource identifier. */
public final class AudioProtocol {
    private static boolean registered;
    private AudioProtocol() {}
    public static synchronized void registerPayloadTypes() {
        if (registered) return;
        PayloadTypeRegistry.clientboundPlay().register(Signal.TYPE, Signal.CODEC);
        registered = true;
    }
    public enum Cue {
        SELECT(""), START("ui.game_start"), STOP(""),
        RESEARCH_COMPLETE("ui.research.complete"), FOCUS_COMPLETE("ui.focus.complete"),
        FOCUS_SELECT("ui.focus.select"), DIVISION_SELECT("ui.division.select"),
        CONSTRUCTION_PLACE("ui.construction.place"),
        MAP_ARMY("ui.map.army"), MAP_NAVY("ui.map.navy"), MAP_AIR("ui.map.air"),
        MAP_SUPPLY("ui.map.supply"), MAP_CONSTRUCTION("ui.map.construction");
        private final String sound;
        Cue(String sound) { this.sound = sound; }
        public String sound() { return sound; }
    }
    public record Signal(Cue cue) implements CustomPacketPayload {
        public Signal { Objects.requireNonNull(cue); }
        public static final Type<Signal> TYPE = new Type<>(Identifier.fromNamespaceAndPath("hoi", "audio_cue_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Signal> CODEC = StreamCodec.of(
                (b,p) -> b.writeEnum(p.cue()), b -> new Signal(b.readEnum(Cue.class)));
        @Override public Type<Signal> type() { return TYPE; }
    }
}
