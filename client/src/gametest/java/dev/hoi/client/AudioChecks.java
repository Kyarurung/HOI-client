package dev.hoi.client;

import dev.hoi.protocol.AudioProtocol;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import java.util.*;

final class AudioChecks {
    static void run(ClientGameTestContext context) {
        context.runOnClient(client -> {
            AudioProtocol.registerPayloadTypes();AudioProtocol.registerPayloadTypes();
            var sounds=client.getSoundManager();var required=new HashSet<String>();
            for(var cue:AudioProtocol.Cue.values()) if(!cue.sound().isEmpty())required.add(cue.sound());
            required.addAll(List.of("music.tfr_theme","ui.menu_tab","ui.click","ui.close","ui.research.select"));
            for(String c:List.of("infantry","support","artillery","armor","navy","air","engineering","industry"))required.add("ui.research.tab."+c);
            var groups=Set.of(UiSounds.construction("air_base"),UiSounds.construction("civilian_factory"),UiSounds.construction("port"));
            if(groups.size()!=3||groups.contains("ui.construction.place"))throw new AssertionError("Construction selection and placement sounds are distinct");
            required.addAll(groups);
            for(String event:required)if(sounds.getSoundEvent(UiSounds.id(event))==null)throw new AssertionError("External sound is missing: "+event);
            for(var cue:AudioProtocol.Cue.values()) {
                var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),net.minecraft.core.RegistryAccess.EMPTY);
                try {
                    var value=new AudioProtocol.Signal(cue);AudioProtocol.Signal.CODEC.encode(buffer,value);
                    if(!value.equals(AudioProtocol.Signal.CODEC.decode(buffer))||buffer.readableBytes()!=0)throw new AssertionError("Audio cue codec");
                } finally {buffer.release();}
            }
            UiSounds.receive(AudioProtocol.Cue.START);UiSounds.reset();
        });
    }
}
