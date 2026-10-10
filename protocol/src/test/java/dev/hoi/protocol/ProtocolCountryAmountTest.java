package dev.hoi.protocol;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class ProtocolCountryAmountTest {
    @Test void actionAmountHasPositiveLongCapacityAndNoAdditionalCountryOrEquipmentAuthority() {
        String token=UUID.randomUUID().toString(),session=UUID.randomUUID().toString();
        assertEquals(Long.MAX_VALUE,new ProtocolCountryPayloads.Action(token,session,2,"lend:issued",Long.MAX_VALUE).amount());
        assertEquals(0,new ProtocolCountryPayloads.Action(token,session,2,"access").amount());
        assertThrows(IllegalArgumentException.class,()->new ProtocolCountryPayloads.Action(token,session,2,"lend:issued",-1));
        assertThrows(IllegalArgumentException.class,()->new ProtocolCountryPayloads.Action(token,session,2,"x".repeat(65),1));
    }
    @Test void oldActionViewOmitsAmountInputAndNewInputDoesNotSetQuantityCap() {
        var action=new Gson().fromJson("{\"id\":\"access\",\"name\":\"Access\",\"detail\":\"\",\"enabled\":true}",ProtocolCountryView.Action.class);
        assertNull(action.amountInput());
        var input=new ProtocolCountryView.AmountInput("Equipment",0);
        assertEquals(0,input.stock());
        assertThrows(IllegalArgumentException.class,()->new ProtocolCountryView.AmountInput("Equipment",-1));
        assertThrows(IllegalArgumentException.class,()->new ProtocolCountryView.AmountInput("x".repeat(129),0));
    }
    @Test void versionTwoCodecRoundTripsLongMaximumAndConsumesTheWholePacket() {
        var request=new ProtocolCountryPayloads.Action(UUID.randomUUID().toString(),UUID.randomUUID().toString(),4,"lend:issued",Long.MAX_VALUE);
        var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),net.minecraft.core.RegistryAccess.EMPTY);
        try {
            ProtocolCountryPayloads.Action.CODEC.encode(buffer,request);
            assertEquals(request,ProtocolCountryPayloads.Action.CODEC.decode(buffer));
            assertEquals(0,buffer.readableBytes());
            assertEquals("hoi:country_action_v2",request.type().id().toString());
        } finally {buffer.release();}
    }
}
