package dev.hoi.protocol;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import java.util.List;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProtocolAgencyPayloadsTest {
    @Test void argumentsRoundTripAtTheLimit() {
        var request = new ProtocolAgencyPayloads.Request(ProtocolAgencyPayloads.Kind.CALL, "session", 7,
                "choice", List.of("가".repeat(128), "둘", "셋", "넷"));
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            ProtocolAgencyPayloads.Request.CODEC.encode(buffer, request);
            assertEquals(request, ProtocolAgencyPayloads.Request.CODEC.decode(buffer));
            assertEquals(0, buffer.readableBytes());
        } finally { buffer.release(); }
    }

    @Test void rejectsOversizedArgumentCountBeforeReadingElements() {
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            buffer.writeEnum(ProtocolAgencyPayloads.Kind.CALL);
            buffer.writeUtf("session");
            buffer.writeVarLong(7);
            buffer.writeUtf("choice");
            buffer.writeVarInt(5);
            assertThrows(DecoderException.class, () -> ProtocolAgencyPayloads.Request.CODEC.decode(buffer));
        } finally { buffer.release(); }
    }
}
