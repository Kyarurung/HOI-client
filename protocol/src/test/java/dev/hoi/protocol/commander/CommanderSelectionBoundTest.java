package dev.hoi.protocol.commander;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CommanderSelectionBoundTest {
    @Test void individualDivisionSelectionsRoundTripAtAuthoritativeLimitAndRejectExcess() {
        var ids=java.util.stream.IntStream.range(0,2048).mapToObj(i->"unit/"+"x".repeat(150)+String.format(java.util.Locale.ROOT,"%04d",i)).toList();var request=new CommanderHudPayloads.Open(CommanderHudPayloads.Mode.CREATE_ARMY,"",ids);
        var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),net.minecraft.core.RegistryAccess.EMPTY);
        try {CommanderHudPayloads.Open.CODEC.encode(buffer,request);assertTrue(buffer.readableBytes()>32767);assertTrue(buffer.readableBytes()<400000);assertEquals(request,CommanderHudPayloads.Open.CODEC.decode(buffer));}finally{buffer.release();}
        var excessive=new ArrayList<>(ids);excessive.add("unit/excess");assertThrows(IllegalArgumentException.class,()->new CommanderHudPayloads.Open(CommanderHudPayloads.Mode.CREATE_ARMY,"",excessive));
        var malformed=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),net.minecraft.core.RegistryAccess.EMPTY);
        try {malformed.writeEnum(CommanderHudPayloads.Mode.CREATE_ARMY);malformed.writeUtf("",160);malformed.writeVarInt(2049);assertThrows(IllegalArgumentException.class,()->CommanderHudPayloads.Open.CODEC.decode(malformed));}finally{malformed.release();}
    }
}
