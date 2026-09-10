package dev.hoi.protocol;

import com.google.gson.Gson;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public final class ConstructionProtocol {
    private static final Gson JSON=new Gson();
    private static boolean registered;
    private ConstructionProtocol() {}
    public static synchronized void registerPayloadTypes() {
        if(registered)return;
        PayloadTypeRegistry.serverboundPlay().register(Request.TYPE,Request.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(CancelMap.TYPE,CancelMap.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Response.TYPE,Response.CODEC);
        registered=true;
    }
    public enum Action { OPEN, REFRESH, SELECT, PLACE, CANCEL, UP, DOWN, REPAIR, CLOSE, CANCEL_AT }
    public record CancelMap() implements CustomPacketPayload {
        public static final CancelMap INSTANCE = new CancelMap();
        public static final Type<CancelMap> TYPE = new Type<>(Identifier.fromNamespaceAndPath("hoi", "construction_cancel_map_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf, CancelMap> CODEC = StreamCodec.unit(INSTANCE);
        @Override public Type<CancelMap> type() { return TYPE; }
    }
    public record Request(Action action,String session,long revision,String item,int amount,double dx,double dy,double dz) implements CustomPacketPayload {
        public Request {
            ConstructionView.text(session,36);ConstructionView.text(item,128);
            if(action==null||revision<0||amount<0||amount>10000)throw new IllegalArgumentException("Invalid construction request");
            for(double n:new double[]{dx,dy,dz})if(!Double.isFinite(n)||Math.abs(n)>1.001)throw new IllegalArgumentException("Invalid map ray");
            if((action==Action.PLACE||action==Action.CANCEL_AT) && Math.abs(dx*dx+dy*dy+dz*dz-1)>.001)throw new IllegalArgumentException("Invalid map ray length");
        }
        public static final Type<Request> TYPE=new Type<>(Identifier.fromNamespaceAndPath("hoi","construction_request_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Request> CODEC=StreamCodec.of(
                (b,p)->{b.writeEnum(p.action);b.writeUtf(p.session,36);b.writeVarLong(p.revision);b.writeUtf(p.item,128);b.writeVarInt(p.amount);b.writeDouble(p.dx);b.writeDouble(p.dy);b.writeDouble(p.dz);},
                b->new Request(b.readEnum(Action.class),b.readUtf(36),b.readVarLong(),b.readUtf(128),b.readVarInt(),b.readDouble(),b.readDouble(),b.readDouble()));
        @Override public Type<Request> type(){return TYPE;}
    }
    public record Response(String json) implements CustomPacketPayload {
        public Response { ConstructionView.text(json,160000); }
        public static final Type<Response> TYPE=new Type<>(Identifier.fromNamespaceAndPath("hoi","construction_response_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Response> CODEC=StreamCodec.of((b,p)->b.writeUtf(p.json,160000),b->new Response(b.readUtf(160000)));
        public static Response of(ConstructionView view){return new Response(JSON.toJson(view));}
        public ConstructionView view() { return PayloadJson.read(JSON, json, ConstructionView.class); }
        @Override public Type<Response> type(){return TYPE;}
    }
}
