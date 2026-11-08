package dev.hoi.protocol.industry;

import dev.hoi.protocol.ProtocolJsonCodec;
import dev.hoi.protocol.map.MapAtlasScene;
import com.google.gson.Gson;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public final class IndustryPayloads {
    private static final Gson JSON=new Gson();
    private static boolean registered;
    private IndustryPayloads() {}
    public enum Action { OPEN, REFRESH, CLOSE, ADD, ASSIGN, SWITCH, REMOVE, FIRST, TRADE, CANCEL_TRADE,
        LOAD_TEMPLATE, NEW_TEMPLATE, LINE_SLOT, SUPPORT_SLOT, SAVE_TEMPLATE, RECRUIT, CANCEL_RECRUIT, PRIORITY, DEPLOY,
        LINE_COLUMN, REGIMENT_SUPPORT, UP, DOWN, DEPLOY_GROUP, CANCEL_GROUP, SERIES, LOCATION, GROUP_PRIORITY, RETIRE_TEMPLATE, ALLOCATION_PRIORITY, SELECT_LOCATION, LOCATION_AT, CONVERSION,
        TANK_OPEN, TANK_MODULE, TANK_UPGRADE, TANK_SAVE, TANK_CLOSE, TANK_RESET, TANK_PRESET, REPAIR_PRIORITY, REPAIR_ENABLED, REPAIR_DOCKYARDS, NAVAL_PORT, NAVAL_LOCATE, HQ_OPEN, HQ_NEW, HQ_EDIT, HQ_COPY, HQ_SLOT, HQ_RESET, HQ_DISCARD, HQ_SAVE, CAPTIVITY_OPEN, CAPTIVITY_QUERY, MANUFACTURER, TEMPLATE_PRIORITY, NAME_GROUP, DUPLICATE_TEMPLATE, OFFER, HQ_PRIORITY, HQ_DELETE, TANK_DESIGN_TEAM, TANK_DESIGN_TEAM_UPGRADE, MOVE, FOREIGN_LOGISTICS }
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
            IndustryView.text(session,36);IndustryView.text(item,128);IndustryView.text(other,action==Action.TANK_PRESET?8192:128);
            if(action==null||revision<0||amount<0)throw new IllegalArgumentException("Invalid industry request");
        }
        public static final Type<Request> TYPE=new Type<>(Identifier.fromNamespaceAndPath("hoi","industry_request_v2"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Request> CODEC=StreamCodec.of(
                (b,p)->{b.writeEnum(p.action);b.writeUtf(p.session,36);b.writeVarLong(p.revision);b.writeUtf(p.item,128);b.writeUtf(p.other,8192);b.writeVarInt(p.amount);b.writeDouble(p.dx);b.writeDouble(p.dy);b.writeDouble(p.dz);},
                b->new Request(b.readEnum(Action.class),b.readUtf(36),b.readVarLong(),b.readUtf(128),b.readUtf(8192),b.readVarInt(),b.readDouble(),b.readDouble(),b.readDouble()));
        @Override public Type<Request> type(){return TYPE;}
    }
    public record DeploymentOverlay(String session,long revision,MapAtlasScene.Page page) implements CustomPacketPayload {
        public DeploymentOverlay {IndustryView.text(session,36);if(revision<0||page==null)throw new IllegalArgumentException("Invalid deployment overlay");}
        public static final Type<DeploymentOverlay> TYPE=new Type<>(Identifier.parse("hoi:deployment_overlay_v2"));
        public static final StreamCodec<RegistryFriendlyByteBuf,DeploymentOverlay> CODEC=StreamCodec.of(
                (b,p)->{b.writeUtf(p.session,36);b.writeVarLong(p.revision);MapAtlasScene.Page.CODEC.encode(b,p.page);},
                b->new DeploymentOverlay(b.readUtf(36),b.readVarLong(),MapAtlasScene.Page.CODEC.decode(b)));
        @Override public Type<DeploymentOverlay> type(){return TYPE;}
    }
    public record Response(String json) implements CustomPacketPayload {
        public Response {IndustryView.text(json,800000);}
        public static final Type<Response> TYPE=new Type<>(Identifier.fromNamespaceAndPath("hoi","industry_response_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Response> CODEC=StreamCodec.of((b,p)->b.writeUtf(p.json,800000),b->new Response(b.readUtf(800000)));
        public static Response of(IndustryView view){return new Response(JSON.toJson(view));}
        public IndustryView view() { return ProtocolJsonCodec.read(JSON, json, IndustryView.class); }
        @Override public Type<Response> type(){return TYPE;}
    }
}
