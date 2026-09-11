package dev.hoi.client.audio;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.*;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;


public final class SuperEventAudio {
    private static String token = "";
    private static SoundInstance sound;
    private SuperEventAudio() {}
    public static void play(String session, String path) {
        if (session.equals(token)) return;
        reset(); if (path.isEmpty()) return;
        var client = Minecraft.getInstance(); var id = Identifier.fromNamespaceAndPath("hoi", path);
        if (client.getSoundManager().getSoundEvent(id) == null) return;
        token = session; client.getMusicManager().stopPlaying();
        sound = new SimpleSoundInstance(id, SoundSource.MASTER, 1, 1, RandomSource.create(), false, 0, SoundInstance.Attenuation.NONE, 0, 0, 0, true);
        client.getSoundManager().play(sound);
    }
    public static boolean active() { return sound != null && Minecraft.getInstance().getSoundManager().isActive(sound); }
    public static void tick() { if (sound != null && Minecraft.getInstance().getSoundManager().isActive(sound)) Minecraft.getInstance().getMusicManager().stopPlaying(); }
    public static void stop(String session) { if (token.equals(session)) reset(); }
    public static void reset() { if (sound != null) Minecraft.getInstance().getSoundManager().stop(sound); sound = null; token = ""; }
}
