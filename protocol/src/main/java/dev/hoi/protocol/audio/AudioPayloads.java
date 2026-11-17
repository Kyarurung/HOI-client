package dev.hoi.protocol.audio;

import dev.hoi.protocol.map.MapFocus;
import dev.hoi.protocol.unit.UnitAudio;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import java.util.Objects;

public final class AudioPayloads {
    private static boolean registered;
    private AudioPayloads() {}
    public static synchronized void registerPayloadTypes() {
        if (registered) return;
        UnitAudio.registerPayloadTypes();
        MapFocus.registerPayloadTypes();
        PayloadTypeRegistry.clientboundPlay().register(Signal.TYPE, Signal.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Music.TYPE, Music.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Sound.TYPE, Sound.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(KoreanMusicRequest.TYPE, KoreanMusicRequest.CODEC);
        registered = true;
    }
    public enum Cue {
        SELECT(""), START("ui.game_start"), STOP(""),
        RESEARCH_COMPLETE("ui.research.complete"), FOCUS_COMPLETE("ui.focus.complete"),
        FOCUS_SELECT("ui.focus.select"), DIVISION_SELECT("ui.division.select"),
        CONSTRUCTION_PLACE("ui.construction.place"),
        MAP_ARMY("ui.map.army"), MAP_NAVY("ui.map.navy"), MAP_AIR("ui.map.air"),
        MAP_SUPPLY("ui.map.supply"), MAP_CONSTRUCTION("ui.map.construction"),
        POLITICS_APPOINT("ui.politics.appoint"), POLITICS_LAW("ui.politics.law"), UI_CLICK("ui.click"),
        COUNTRY_SELECT("ui.country.select"), ORDER_CONVOY_TRANSPORT("ui.order.convoy_transport");
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
    public record Music(String event) implements CustomPacketPayload {
        public Music {
            if(event==null || event.length()>128 || !event.matches("music\\.(korea|story)\\.[a-z0-9_.]+"))throw new IllegalArgumentException("Invalid music event");
        }
        public static final Type<Music> TYPE=new Type<>(Identifier.fromNamespaceAndPath("hoi","story_music_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Music> CODEC=StreamCodec.of((b,p) -> b.writeUtf(p.event(),128),b -> new Music(b.readUtf(128)));
        @Override public Type<Music> type(){return TYPE;}
    }
    public record Sound(String event) implements CustomPacketPayload {
        public Sound {
            if(event==null || event.length()>128 || !event.matches("sound\\.story\\.[a-z0-9_.]+"))throw new IllegalArgumentException("Invalid sound event");
        }
        public static final Type<Sound> TYPE=new Type<>(Identifier.fromNamespaceAndPath("hoi","story_sound_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Sound> CODEC=StreamCodec.of((b,p) -> b.writeUtf(p.event(),128),b -> new Sound(b.readUtf(128)));
        @Override public Type<Sound> type(){return TYPE;}
    }
    public record KoreanMusicRequest() implements CustomPacketPayload {
        public static final KoreanMusicRequest INSTANCE=new KoreanMusicRequest();
        public static final Type<KoreanMusicRequest> TYPE=new Type<>(Identifier.fromNamespaceAndPath("hoi","korean_music_request_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf,KoreanMusicRequest> CODEC=StreamCodec.unit(INSTANCE);
        @Override public Type<KoreanMusicRequest> type(){return TYPE;}
    }
}
