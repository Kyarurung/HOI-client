package dev.hoi.protocol.commander;

import com.google.gson.Gson;
import dev.hoi.protocol.ProtocolJsonCodec;
import dev.hoi.protocol.industry.IndustryTankDesigner;
import java.util.*;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public final class CommanderShipPayloads {
    private static final Gson JSON=new Gson();
    private static boolean registered;
    public static final int ROWS=256;
    private CommanderShipPayloads() {}
    public enum Action { LIST, OPEN, CLOSE_LIST, CLOSE }
    public record Request(String token,Action action,String target) implements CustomPacketPayload {
        public Request {UUID.fromString(token);Objects.requireNonNull(action);identifier(target);if(target.isEmpty())throw new IllegalArgumentException("Missing ship target");}
        public static final Type<Request> TYPE=new Type<>(Identifier.parse("hoi:commander_ship_request_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Request> CODEC=StreamCodec.of((b,p)->{b.writeUtf(p.token,36);b.writeEnum(p.action);b.writeUtf(p.target,160);},b->new Request(b.readUtf(36),b.readEnum(Action.class),b.readUtf(160)));
        @Override public Type<Request> type(){return TYPE;}
    }
    public record Row(String id,String name,String type,String icon,int level,double progress,double strength,double organization,String strengthTooltip,String organizationTooltip) {
        public Row {identifier(id);text(name,160);text(type,200);asset(icon);text(strengthTooltip,400);text(organizationTooltip,400);if(level<0||level>4||!ratio(progress)||!ratio(strength)||!ratio(organization))throw new IllegalArgumentException("Invalid ship row");}
    }
    public record Force(String token,String force,List<Row> ships,Map<String,String> labels,String error) {
        public Force {UUID.fromString(token);identifier(force);ships=List.copyOf(ships);labels=Map.copyOf(labels);text(error,400);if(ships.size()>ROWS||labels.size()>16)throw new IllegalArgumentException("Invalid ship list");labels.forEach((key,value)->{identifier(key);text(value,1000);});}
        public String label(String key){return labels.getOrDefault(key,"");}
    }
    public record Column(String id,List<IndustryTankDesigner.Stat> stats) {
        public Column {identifier(id);stats=List.copyOf(stats);if(stats.size()>16)throw new IllegalArgumentException("Invalid ship statistics");}
    }
    public record Wing(String name,String icon,String amount) {
        public Wing {text(name,160);asset(icon);text(amount,32);}
    }
    public record View(String token,String ship,String country,String name,String type,String design,String designName,boolean designer,String image,int level,String manpower,String fuel,String scrap,
            CommanderDivisionPayloads.Officer officer,List<Column> columns,List<Wing> wings,String wingTotal,Map<String,String> tooltips,Map<String,String> labels,String error) {
        public View {UUID.fromString(token);identifier(ship);identifier(country);text(name,160);asset(type);identifier(design);text(designName,160);asset(image);text(manpower,32);text(fuel,32);text(scrap,400);text(wingTotal,64);text(error,400);
            columns=List.copyOf(columns);wings=List.copyOf(wings);tooltips=Map.copyOf(tooltips);labels=Map.copyOf(labels);
            if(level<0||level>4||columns.size()>3||wings.size()>32||tooltips.size()>16||labels.size()>32)throw new IllegalArgumentException("Invalid ship view");
            labels.forEach((key,value)->{identifier(key);text(value,1000);});tooltips.forEach((key,value)->{identifier(key);text(value,2000);});}
        public String label(String key){return labels.getOrDefault(key,"");}
        public String tooltip(String key){return tooltips.getOrDefault(key,"");}
    }
    public record ForceSnapshot(String json) implements CustomPacketPayload {
        public ForceSnapshot {bounded(json,200000);}
        public static final Type<ForceSnapshot> TYPE=new Type<>(Identifier.parse("hoi:commander_ship_list_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf,ForceSnapshot> CODEC=StreamCodec.of((b,p)->b.writeUtf(p.json,200000),b->new ForceSnapshot(b.readUtf(200000)));
        public static ForceSnapshot of(Force force){return new ForceSnapshot(JSON.toJson(force));}
        public Force force(){return ProtocolJsonCodec.read(JSON,json,Force.class);}
        @Override public Type<ForceSnapshot> type(){return TYPE;}
    }
    public record Snapshot(String json) implements CustomPacketPayload {
        public Snapshot {bounded(json,65536);}
        public static final Type<Snapshot> TYPE=new Type<>(Identifier.parse("hoi:commander_ship_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Snapshot> CODEC=StreamCodec.of((b,p)->b.writeUtf(p.json,65536),b->new Snapshot(b.readUtf(65536)));
        public static Snapshot of(View view){return new Snapshot(JSON.toJson(view));}
        public View view(){return ProtocolJsonCodec.read(JSON,json,View.class);}
        @Override public Type<Snapshot> type(){return TYPE;}
    }
    public static synchronized void registerPayloadTypes(){if(registered)return;PayloadTypeRegistry.serverboundPlay().register(Request.TYPE,Request.CODEC);PayloadTypeRegistry.clientboundPlay().register(ForceSnapshot.TYPE,ForceSnapshot.CODEC);PayloadTypeRegistry.clientboundPlay().register(Snapshot.TYPE,Snapshot.CODEC);registered=true;}
    private static void bounded(String json,int bound){text(json,bound);if(json.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>bound)throw new IllegalArgumentException("Ship packet exceeds byte budget");}
    private static void text(String s,int bound){if(s==null||s.length()>bound)throw new IllegalArgumentException("Invalid ship text");}
    private static void identifier(String s){text(s,160);if(!s.isEmpty()&&!s.matches("[A-Za-z0-9_:/|.-]+"))throw new IllegalArgumentException("Invalid ship ID");}
    private static boolean ratio(double v){return Double.isFinite(v)&&v>=0&&v<=1;}
    private static void asset(String s){text(s,180);if(!s.isEmpty()&&!s.matches("[a-z0-9_/-]+"))throw new IllegalArgumentException("Invalid ship asset");}
}
