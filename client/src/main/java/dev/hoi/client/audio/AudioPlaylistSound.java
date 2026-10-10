package dev.hoi.client.audio;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

final class AudioPlaylistSound extends AbstractTickableSoundInstance {
    AudioPlaylistSound(String path) {
        super(SoundEvent.createVariableRangeEvent(AudioUiSounds.id(path)), SoundSource.MASTER, RandomSource.create());
        attenuation = SoundInstance.Attenuation.NONE;
        relative = true;
    }
    @Override public float getVolume() {
        return super.getVolume() * (float)AudioKoreanMusic.volume();
    }
    @Override public boolean canStartSilent() {
        return true;
    }
    @Override public void tick() {}
}
