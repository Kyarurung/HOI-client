package dev.hoi.client.mixin;

import java.util.Map;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SoundEngine.class)
public interface MixinEngineAccess {
    @Accessor("soundDeleteTime") Map<SoundInstance,Integer> hoiSoundDeleteTime();
    @Accessor("instanceBySource") com.google.common.collect.Multimap<net.minecraft.sounds.SoundSource,SoundInstance> hoiSoundsBySource();
    @Accessor("tickingSounds") java.util.List<net.minecraft.client.resources.sounds.TickableSoundInstance> hoiTickingSounds();
    @Accessor("queuedSounds") Map<SoundInstance,Integer> hoiQueuedSounds();
    @Accessor("queuedTickableSounds") java.util.List<net.minecraft.client.resources.sounds.TickableSoundInstance> hoiQueuedTickableSounds();
    @Accessor("instanceToChannel") Map<SoundInstance,ChannelAccess.ChannelHandle> hoiChannels();
}
