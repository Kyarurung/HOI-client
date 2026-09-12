package dev.hoi.client.audio;

import dev.hoi.protocol.UnitAudioProtocol;
import dev.hoi.protocol.UnitAudioProtocol.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import java.util.*;

public final class UnitAudio {
    private static final Map<String,Voice> voices=new HashMap<>();
    private static int age;
    private static String dimension="";
    private UnitAudio() {}
    public static void receive(Snapshot snapshot) {
        var client=Minecraft.getInstance();
        if(client.level==null||!client.level.dimension().identifier().toString().equals(snapshot.dimension())) {reset();return;}
        if(!dimension.equals(snapshot.dimension()))reset();
        dimension=snapshot.dimension();age=0;
        var retained=new HashSet<String>();
        for(var e:snapshot.emitters()) {
            if(!audible(e))continue;
            retained.add(e.id());var voice=voices.get(e.id());
            if(voice!=null&&(voice.emitter.sound()!=e.sound()||voice.isStopped())) {voice.cancel();voices.remove(e.id());voice=null;}
            if(voice==null&&client.getSoundManager().getSoundEvent(UiSounds.id(e.sound().event()))!=null) {
                voice=new Voice(e);voices.put(e.id(),voice);client.getSoundManager().play(voice);
            } else if(voice!=null)voice.update(e);
        }
        voices.entrySet().removeIf(entry->{if(retained.contains(entry.getKey()))return false;entry.getValue().cancel();return true;});
    }
    private static boolean audible(Emitter emitter) {
        var client=Minecraft.getInstance();
        return client.player!=null&&emitter.distanceSquared(client.player.getX(),client.player.getY(),client.player.getZ())
                <=UnitAudioProtocol.RANGE*UnitAudioProtocol.RANGE;
    }
    public static void tick() {
        var client=Minecraft.getInstance();
        if(client.level==null||!dimension.equals(client.level.dimension().identifier().toString())||++age>40) {reset();return;}
        voices.entrySet().removeIf(entry->{var voice=entry.getValue();if(!voice.isStopped()&&audible(voice.emitter))return false;voice.cancel();return true;});
    }
    public static void reset() { voices.values().forEach(Voice::cancel);voices.clear();dimension="";age=0; }
    static int count() {return voices.size();}
    static final class Voice extends AbstractTickableSoundInstance {
        private Emitter emitter;
        Voice(Emitter emitter) {
            super(SoundEvent.createFixedRangeEvent(UiSounds.id(emitter.sound().event()),UnitAudioProtocol.RANGE),SoundSource.MASTER,RandomSource.create());
            looping=true;delay=emitter.sound().combat()?20:0;volume=.8f;pitch=1;relative=false;
            update(emitter);
        }
        void update(Emitter value) {emitter=value;x=value.x();y=value.y();z=value.z();}
        @Override public boolean canPlaySound() {return !isStopped()&&audible(emitter);}
        @Override public void tick() {if(!audible(emitter))cancel();}
        void cancel() {looping=false;volume=0;stop();Minecraft.getInstance().getSoundManager().stop(this);}
    }
}
