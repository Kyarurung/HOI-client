package dev.hoi.client.audio;

import dev.hoi.protocol.ProtocolAudioPayloads;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import java.util.*;

public final class AudioChecks {
    public static void run(ClientGameTestContext context) {
        context.runOnClient(client -> {
            ProtocolAudioPayloads.registerPayloadTypes();ProtocolAudioPayloads.registerPayloadTypes();
            var sounds=client.getSoundManager();var required=new HashSet<String>();
            for(var cue:ProtocolAudioPayloads.Cue.values()) if(!cue.sound().isEmpty())required.add(cue.sound());
            required.addAll(List.of("music.tfr_theme","ui.menu_tab","ui.click","ui.close","ui.research.select","ui.checkbox","ui.scroll","ui.agency.intel","ui.agency.defense","ui.agency.operations","ui.agency.training","ui.agency.cryptology"));
            for(String c:List.of("infantry","support","artillery","armor","navy","air","engineering","industry"))required.add("ui.research.tab."+c);
            if (!AudioUiSounds.START_SOUNDS.equals(List.of("ui.game_start", "ui.game_start_signal")))
                throw new AssertionError("Both original start samples must play, not random variants");
            required.addAll(AudioUiSounds.START_SOUNDS);
            required.addAll(List.of("ui.agency.decrypt.hover", "ui.agency.decrypt.select"));
            for (String event : required) {
                if (AudioUiSounds.instance(event).getSource() != net.minecraft.sounds.SoundSource.MASTER)
                    throw new AssertionError("All HOI UI sounds use MASTER: " + event);
            }
            if (dev.hoi.client.ui.UiEffectColors.color(0xFF55FF55) != dev.hoi.client.ui.UiEffectColors.GOOD
                    || dev.hoi.client.ui.UiEffectColors.color(0x80AA0000) != 0x80CB9292
                    || dev.hoi.client.ui.UiEffectColors.color(0xFFFFAA00) != 0xFFFFAA00)
                throw new AssertionError("HUD effect palette preserves alpha and neutral colors");
            for(String event:required)if(sounds.getSoundEvent(AudioUiSounds.id(event))==null)throw new AssertionError("External sound is missing: "+event);
            for(var cue:ProtocolAudioPayloads.Cue.values()) {
                var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),net.minecraft.core.RegistryAccess.EMPTY);
                try {
                    var value=new ProtocolAudioPayloads.Signal(cue);ProtocolAudioPayloads.Signal.CODEC.encode(buffer,value);
                    if(!value.equals(ProtocolAudioPayloads.Signal.CODEC.decode(buffer))||buffer.readableBytes()!=0)throw new AssertionError("Audio cue codec");
                } finally {buffer.release();}
            }
            AudioUiSounds.receive(ProtocolAudioPayloads.Cue.START);AudioUiSounds.reset();
        });
    }
}
