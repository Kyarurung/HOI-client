package dev.hoi.protocol;

import com.google.gson.Gson;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public final class IndustryProtocol {
    private static final Gson JSON=new Gson();
    private static boolean registered;
    private IndustryProtocol() {}
    public enum Action { OPEN, REFRESH, CLOSE, ADD, ASSIGN, SWITCH, REMOVE, FIRST, TRADE, CANCEL_TRADE,
        LOAD_TEMPLATE, NEW_TEMPLATE, LINE_SLOT, SUPPORT_SLOT, SAVE_TEMPLATE, RECRUIT, CANCEL_RECRUIT, PRIORITY, DEPLOY,
        LINE_COLUMN, REGIMENT_SUPPORT, UP, DOWN, DEPLOY_GROUP, CANCEL_GROUP, SERIES, LOCATION, GROUP_PRIORITY, RETIRE_TEMPLATE, ALLOCATION_PRIORITY, SELECT_LOCATION, LOCATION_AT }
    public static synchronized void registerPayloadTypes() {
        if(registered)return;
        PayloadTypeRegistry.serverboundPlay().register(Request.TYPE,Request.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(DeploymentOverlay.TYPE,DeploymentOverlay.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Response.TYPE,Response.CODEC);registered=true;
    }
    public record Request(Action action,String session,long revision,String item,String other,int amount,double dx,double dy,double dz) implements CustomPacketPayload {
        public Request(Action action,String session,long revision,String item,String other,int amount) {this(action,session,revision,item,other,amount,0,0,0);}
        public Request {
            for(double n:new double[]{dx,dy,dz})if(!Double.isFinite(n)||Math.abs(n)>1)throw new IllegalArgumentException("Invalid deployment ray");
            IndustryView.text(session,36);IndustryView.text(item,128);IndustryView.text(other,128);
            if(action==null||revision<0||amount<0)throw new IllegalArgumentException("Invalid industry request");
        }
        public static final Type<Request> TYPE=new Type<>(Identifier.fromNamespaceAndPath("hoi","industry_request_v3"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Request> CODEC=StreamCodec.of(
                (b,p)->{b.writeEnum(p.action);b.writeUtf(p.session,36);b.writeVarLong(p.revision);b.writeUtf(p.item,128);b.writeUtf(p.other,128);b.writeVarInt(p.amount);b.writeDouble(p.dx);b.writeDouble(p.dy);b.writeDouble(p.dz);},
                b->new Request(b.readEnum(Action.class),b.readUtf(36),b.readVarLong(),b.readUtf(128),b.readUtf(128),b.readVarInt(),b.readDouble(),b.readDouble(),b.readDouble()));
        @Override public Type<Request> type(){return TYPE;}
    }
    public record DeploymentOverlay(String session,long revision,AtlasSceneProtocol.Page page) implements CustomPacketPayload {
        public DeploymentOverlay {IndustryView.text(session,36);if(revision<0||page==null)throw new IllegalArgumentException("Invalid deployment overlay");}
        public static final Type<DeploymentOverlay> TYPE=new Type<>(Identifier.parse("hoi:deployment_overlay_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf,DeploymentOverlay> CODEC=StreamCodec.of(
                (b,p)->{b.writeUtf(p.session,36);b.writeVarLong(p.revision);AtlasSceneProtocol.Page.CODEC.encode(b,p.page);},
                b->new DeploymentOverlay(b.readUtf(36),b.readVarLong(),AtlasSceneProtocol.Page.CODEC.decode(b)));
        @Override public Type<DeploymentOverlay> type(){return TYPE;}
    }
    public record Response(String json) implements CustomPacketPayload {
        public Response {IndustryView.text(json,800000);}
        public static final Type<Response> TYPE=new Type<>(Identifier.fromNamespaceAndPath("hoi","industry_response_v3"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Response> CODEC=StreamCodec.of((b,p)->b.writeUtf(p.json,800000),b->new Response(b.readUtf(800000)));
        public static Response of(IndustryView view){return new Response(JSON.toJson(view));}
        public IndustryView view(){var v=JSON.fromJson(json,IndustryView.class);if(v==null)throw new IllegalArgumentException("Missing industry view");return v;}
        @Override public Type<Response> type(){return TYPE;}
    }
}
