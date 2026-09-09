package dev.hoi.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.*;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/** One streamed, non-looping original cue per opened super event; updates never replay it. */
final class SuperEventAudio {
    private static String token = "";
    private static SoundInstance sound;
    private SuperEventAudio() {}
    static void play(String session, String path) {
        if (session.equals(token)) return;
        reset(); if (path.isEmpty()) return;
        var client = Minecraft.getInstance(); var id = Identifier.fromNamespaceAndPath("hoi", path);
        if (client.getSoundManager().getSoundEvent(id) == null) return;
        token = session; client.getMusicManager().stopPlaying();
        sound = new SimpleSoundInstance(id, SoundSource.MUSIC, 1, 1, RandomSource.create(), false, 0, SoundInstance.Attenuation.NONE, 0, 0, 0, true);
        client.getSoundManager().play(sound);
    }
    static void tick() { if (sound != null && Minecraft.getInstance().getSoundManager().isActive(sound)) Minecraft.getInstance().getMusicManager().stopPlaying(); }
    static void stop(String session) { if (token.equals(session)) reset(); }
    static void reset() { if (sound != null) Minecraft.getInstance().getSoundManager().stop(sound); sound = null; token = ""; }
}
