package dev.hoi.protocol;

import com.google.gson.Gson;
import java.util.*;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public final class ProtocolFocusPayloads {
    public static final int MAX_JSON=400_000,MAX_NODES=1024;
    private static final Gson JSON=new Gson();
    private static boolean registered;
    private ProtocolFocusPayloads() {}
    public static synchronized void registerPayloadTypes() {
        if(registered)return;
        PayloadTypeRegistry.serverboundPlay().register(Request.TYPE,Request.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Response.TYPE,Response.CODEC);registered=true;
    }
    public enum Action {OPEN,REFRESH,START,CANCEL}
    public record Request(Action action,String token,String session,long revision,String id) implements CustomPacketPayload {
        public Request {
            Objects.requireNonNull(action);UUID.fromString(token);if(!session.isEmpty())UUID.fromString(session);ProtocolCountryView.text(id,256);
            if(revision<0)throw new IllegalArgumentException("Invalid focus revision");
        }
        public static final Type<Request> TYPE=new Type<>(Identifier.parse("hoi:focus_request_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Request> CODEC=StreamCodec.of(
                (b,p) -> {b.writeEnum(p.action);b.writeUtf(p.token,36);b.writeUtf(p.session,36);b.writeVarLong(p.revision);b.writeUtf(p.id,256);},
                b -> new Request(b.readEnum(Action.class),b.readUtf(36),b.readUtf(36),b.readVarLong(),b.readUtf(256)));
        @Override public Type<Request> type(){return TYPE;}
    }
    public record Node(String id,String name,String description,String icon,double x,double y,List<List<Integer>> parents,List<Integer> exclusive,String status,long remaining,boolean enabled) {
        public Node {
            for(String s:List.of(id,name,icon,status))ProtocolCountryView.text(s,256);ProtocolCountryView.text(description,600);
            parents=parents.stream().map(List::copyOf).toList();exclusive=List.copyOf(exclusive);
            if(!Double.isFinite(x)||!Double.isFinite(y)||Math.abs(x)>100000||Math.abs(y)>100000||remaining<0 || parents.size()>64 || exclusive.size()>64 || parents.stream().anyMatch(g -> g.size()>64))throw new IllegalArgumentException("Invalid focus node");
        }
    }
    public enum Assessment {MET,UNMET,NOT_EVALUATED,UNKNOWN}
    public record DetailLine(String text,int depth,Assessment assessment,String sourceKey,boolean supported) {
        public DetailLine {
            ProtocolCountryView.text(text,4096);ProtocolCountryView.text(sourceKey,256);Objects.requireNonNull(assessment);
            if(depth<0 || depth>16)throw new IllegalArgumentException("Invalid focus detail depth");
        }
    }
    public record Detail(String id,String name,String description,long durationHours,long elapsedHours,int bankedHours,int paidBankedHours,double politicalPowerPerDay,String source,int line,List<DetailLine> requirements,List<DetailLine> rewards,boolean executable,String limitation,boolean complete) {
        public Detail {
            ProtocolCountryView.text(id,256);ProtocolCountryView.text(name,512);ProtocolCountryView.text(description,16384);
            ProtocolCountryView.text(source,512);ProtocolCountryView.text(limitation,2048);
            requirements=List.copyOf(requirements);rewards=List.copyOf(rewards);
            if(durationHours<0 || durationHours>1_680_000 || elapsedHours<0 || elapsedHours>durationHours
                    || bankedHours<0 || bankedHours>336 || paidBankedHours<0 || paidBankedHours>bankedHours
                    || !Double.isFinite(politicalPowerPerDay) || politicalPowerPerDay<0 || line<1
                    || requirements.size()>128 || rewards.size()>128)throw new IllegalArgumentException("Invalid focus detail");
            int characters=name.length()+description.length()+source.length()+limitation.length();
            for(var row:requirements)characters+=row.text().length()+row.sourceKey().length();
            for(var row:rewards)characters+=row.text().length()+row.sourceKey().length();
            if(characters>65536)throw new IllegalArgumentException("Focus details exceed budget");
        }
    }
    public record Ack(Action action,String session,long revision,String id) {
        public Ack {
            Objects.requireNonNull(action);if(!session.isEmpty())UUID.fromString(session);ProtocolCountryView.text(id,256);
            if(revision<0)throw new IllegalArgumentException("Invalid focus acknowledgement");
        }
        public static Ack of(Request request) {return new Ack(request.action(),request.session(),request.revision(),request.id());}
    }
    public record View(String country,String name,String tree,String session,long revision,String message,ProtocolCountryHud hud,List<Node> nodes,boolean cancelable,Detail detail,Ack ack) {
        public View(String country,String name,String tree,String session,long revision,String message,ProtocolCountryHud hud,List<Node> nodes,boolean cancelable) {
            this(country,name,tree,session,revision,message,hud,nodes,cancelable,null,null);
        }
        public View {
            ProtocolCountryView.tag(country);UUID.fromString(session);ProtocolCountryView.text(name,256);ProtocolCountryView.text(tree,256);ProtocolCountryView.text(message,512);Objects.requireNonNull(hud);nodes=List.copyOf(nodes);
            if(revision<0||nodes.size()>MAX_NODES||nodes.stream().map(Node::id).distinct().count()!=nodes.size())throw new IllegalArgumentException("Invalid focus graph");
            for(var n:nodes) {
                for(var g:n.parents())for(int p:g)if(p<0||p>=nodes.size())throw new IllegalArgumentException("Invalid focus prerequisite");
                for(int p:n.exclusive())if(p<0||p>=nodes.size())throw new IllegalArgumentException("Invalid focus exclusion");
            }
            if(detail!=null && nodes.stream().noneMatch(n -> n.id().equals(detail.id())))throw new IllegalArgumentException("Focus detail is outside the visible graph");
        }
    }
    public record Response(String token,String json) implements CustomPacketPayload {
        public Response {UUID.fromString(token);ProtocolCountryView.text(json,MAX_JSON);if(json.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>900000)throw new IllegalArgumentException("Focus payload too large");}
        public static final Type<Response> TYPE=new Type<>(Identifier.parse("hoi:focus_response_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Response> CODEC=StreamCodec.of((b,p) -> {b.writeUtf(p.token,36);b.writeUtf(p.json,MAX_JSON);},b -> new Response(b.readUtf(36),b.readUtf(MAX_JSON)));
        public static Response of(String token,View view){return new Response(token,JSON.toJson(view));}
        public View view(){
            try {return ProtocolJsonCodec.read(JSON,json,View.class);}
            catch(RuntimeException e){throw new IllegalArgumentException("Invalid focus view",e);}
        }
        @Override public Type<Response> type(){return TYPE;}
    }
}
