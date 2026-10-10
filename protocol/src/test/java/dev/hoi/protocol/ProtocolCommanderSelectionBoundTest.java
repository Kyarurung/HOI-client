package dev.hoi.protocol;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProtocolCommanderSelectionBoundTest {
    @Test void individualDivisionSelectionsRoundTripAtAuthoritativeLimitAndRejectExcess() {
        var ids=java.util.stream.IntStream.range(0,2048).mapToObj(i->"unit/"+"x".repeat(150)+String.format(java.util.Locale.ROOT,"%04d",i)).toList();var request=new ProtocolCommanderHud.Open(ProtocolCommanderHud.Mode.CREATE_ARMY,"",ids);
        var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),net.minecraft.core.RegistryAccess.EMPTY);
        try {ProtocolCommanderHud.Open.CODEC.encode(buffer,request);assertTrue(buffer.readableBytes()>32767);assertTrue(buffer.readableBytes()<400000);assertEquals(request,ProtocolCommanderHud.Open.CODEC.decode(buffer));}finally{buffer.release();}
        var excessive=new ArrayList<>(ids);excessive.add("unit/excess");assertThrows(IllegalArgumentException.class,()->new ProtocolCommanderHud.Open(ProtocolCommanderHud.Mode.CREATE_ARMY,"",excessive));
        var malformed=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),net.minecraft.core.RegistryAccess.EMPTY);
        try {malformed.writeEnum(ProtocolCommanderHud.Mode.CREATE_ARMY);malformed.writeUtf("",160);malformed.writeVarInt(2049);assertThrows(IllegalArgumentException.class,()->ProtocolCommanderHud.Open.CODEC.decode(malformed));}finally{malformed.release();}
    }
}
