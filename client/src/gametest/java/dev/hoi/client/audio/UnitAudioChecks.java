package dev.hoi.client.audio;

import dev.hoi.protocol.UnitAudioProtocol;
import dev.hoi.protocol.UnitAudioProtocol.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.resources.sounds.SoundInstance;
import java.util.*;

public final class UnitAudioChecks {
    public static void run(ClientGameTestContext context) {
        var played=new SoundInstance[1];var volume=new double[1];
        context.setScreen(()->null);
        context.runOnClient(client->{
            volume[0]=client.options.getSoundSourceVolume(net.minecraft.sounds.SoundSource.HOSTILE);
            client.options.getSoundSourceOptionInstance(net.minecraft.sounds.SoundSource.HOSTILE).set(1.0);
            for(var sound:Sound.values()) {
                var event=client.getSoundManager().getSoundEvent(UiSounds.id(sound.event()));
                if(event==null)throw new AssertionError("Missing positional source sound: "+sound);
                var sample=event.getSound(net.minecraft.util.RandomSource.create(0));
                if(sample.getAttenuationDistance()!=7)throw new AssertionError("Unit sample attenuation radius must be seven");
            }
            var emitter=new Emitter("tank",Sound.TANK,client.player.getX()+1,client.player.getY(),client.player.getZ());
            var snapshot=new Snapshot(client.level.dimension().identifier().toString(),List.of(emitter));
            var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),net.minecraft.core.RegistryAccess.EMPTY);
            try {
                Snapshot.CODEC.encode(buffer,snapshot);
                if(!snapshot.equals(Snapshot.CODEC.decode(buffer))||buffer.readableBytes()!=0)throw new AssertionError("Unit audio codec");
                buffer.clear();buffer.writeUtf(snapshot.dimension(),128);buffer.writeByte(255);
                try {Snapshot.CODEC.decode(buffer);throw new AssertionError("Unbounded sound count accepted");}
                catch(IllegalArgumentException expected) {}
            } finally {buffer.release();}
            UnitAudio.receive(snapshot);
            if(UnitAudio.count()!=1)throw new AssertionError("Nearby tank starts one voice");
            try {
                var field=UnitAudio.class.getDeclaredField("voices");field.setAccessible(true);
                played[0]=(SoundInstance)((Map<?,?>)field.get(null)).get("tank");
                UnitAudio.receive(snapshot);
                if(((Map<?,?>)field.get(null)).get("tank")!=played[0])throw new AssertionError("Unchanged snapshot must reuse the playing sound");
            } catch(ReflectiveOperationException e) {throw new AssertionError(e);}
        });
        context.waitTicks(10);
        context.runOnClient(client->{
            if(!client.getSoundManager().isActive(played[0])||played[0].isRelative()||played[0].getAttenuation()!=SoundInstance.Attenuation.LINEAR)
                throw new AssertionError("Original tank sound must play spatially");
            var before=client.player.position();
            client.player.setPos(played[0].getX()+7.01,played[0].getY(),played[0].getZ());
            UnitAudio.tick();client.player.setPos(before);
            if(UnitAudio.count()!=0||played[0].getVolume()!=0||played[0].isLooping()||played[0].canPlaySound())throw new AssertionError("Crossing seven blocks must immediately silence and prevent replay");
        });
        context.waitTicks(25);
        context.runOnClient(client->{
            if(client.getSoundManager().isActive(played[0]))throw new AssertionError("Out-of-range sound remains active");
            var p=client.player;String dimension=client.level.dimension().identifier().toString();
            UnitAudio.receive(new Snapshot(dimension,List.of(new Emitter("rifle",Sound.RIFLE,p.getX(),p.getY(),p.getZ()))));
            UnitAudio.receive(new Snapshot(dimension,List.of()));
            if(UnitAudio.count()!=0)throw new AssertionError("Ceasefire/paused snapshot must stop combat audio");
            UnitAudio.receive(new Snapshot(dimension,List.of(new Emitter("rifle",Sound.RIFLE,p.getX(),p.getY(),p.getZ()))));
            UnitAudio.receive(new Snapshot("hoi:another_dimension",List.of()));
            if(UnitAudio.count()!=0)throw new AssertionError("Dimension transition must clear voices");
            UnitAudio.reset();client.options.getSoundSourceOptionInstance(net.minecraft.sounds.SoundSource.HOSTILE).set(volume[0]);
        });
    }
}
