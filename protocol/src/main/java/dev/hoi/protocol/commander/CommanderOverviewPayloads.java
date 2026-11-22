package dev.hoi.protocol.commander;

import com.google.gson.Gson;
import dev.hoi.protocol.ProtocolJsonCodec;
import java.util.*;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public final class CommanderOverviewPayloads {
    private static final Gson JSON=new Gson();
    private static boolean registered;
    public static final int ROWS=3000;
    private CommanderOverviewPayloads() {}
    public enum Tab { ARMY, NAVY, AIR }
    public enum Action { OPEN, CLOSE, FOCUS }
    public record Request(String token,Action action,Tab tab,String target) implements CustomPacketPayload {
        public Request {UUID.fromString(token);Objects.requireNonNull(action);Objects.requireNonNull(tab);target=target==null?"":target;identifier(target);}
        public static final Type<Request> TYPE=new Type<>(Identifier.parse("hoi:commander_overview_request_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Request> CODEC=StreamCodec.of((b,p)->{b.writeUtf(p.token,36);b.writeEnum(p.action);b.writeEnum(p.tab);b.writeUtf(p.target,160);},b->new Request(b.readUtf(36),b.readEnum(Action.class),b.readEnum(Tab.class),b.readUtf(160)));
        @Override public Type<Request> type(){return TYPE;}
    }
    public record Modifier(String icon,String value,String tooltip) {
        public Modifier {asset(icon);text(value,64);text(tooltip,2000);}
    }
    public record Count(String key,String icon,int count,String tooltip) {
        public Count {identifier(key);asset(icon);text(tooltip,2000);if(count<0||count>1000000)throw new IllegalArgumentException("Invalid overview count");}
    }
    public record Row(String id,String type,String icon,String name,String location,String group,Integer color,Integer insignia,double organization,double strength,int level,boolean fighting,boolean obsolete,String leader,String amount,List<String> missions,String tooltip,String selection) {
        public Row {identifier(id);identifier(type);asset(icon);text(name,160);text(location,160);text(group,160);asset(leader);text(amount,32);text(tooltip,4000);selection=selection==null?"":selection;identifier(selection);missions=List.copyOf(missions);missions.forEach(CommanderOverviewPayloads::identifier);
            if(color!=null&&(color<0||color>0xffffff)||insignia!=null&&(insignia<0||insignia>80)||!ratio(organization)||!ratio(strength)||level<0||level>4||missions.size()>4)throw new IllegalArgumentException("Invalid overview row");}
    }
    public record View(String token,Tab tab,String country,String total,String secondary,String special,String specialTooltip,List<Modifier> modifiers,List<Count> types,List<Row> rows,String footer,Map<String,String> labels,String error) {
        public View {UUID.fromString(token);Objects.requireNonNull(tab);text(country,8);text(total,80);text(secondary,80);text(special,32);text(specialTooltip,2000);text(footer,160);text(error,400);modifiers=modifiers==null?List.of():List.copyOf(modifiers);types=List.copyOf(types);rows=List.copyOf(rows);labels=Map.copyOf(labels);
            if(modifiers.size()>14||types.size()>32||rows.size()>ROWS||labels.size()>32)throw new IllegalArgumentException("Invalid overview size");labels.forEach((key,value)->{identifier(key);text(value,2000);});}
        public String label(String key){return labels.getOrDefault(key,"");}
    }
    public record Snapshot(String json) implements CustomPacketPayload {
        public Snapshot {text(json,1000000);if(json.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>1000000)throw new IllegalArgumentException("Overview packet exceeds byte budget");}
        public static final Type<Snapshot> TYPE=new Type<>(Identifier.parse("hoi:commander_overview_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Snapshot> CODEC=StreamCodec.of((b,p)->b.writeUtf(p.json,1000000),b->new Snapshot(b.readUtf(1000000)));
        public static Snapshot of(View view){return new Snapshot(JSON.toJson(view));}
        public View view(){return ProtocolJsonCodec.read(JSON,json,View.class);}
        @Override public Type<Snapshot> type(){return TYPE;}
    }
    public static synchronized void registerPayloadTypes(){if(registered)return;PayloadTypeRegistry.serverboundPlay().register(Request.TYPE,Request.CODEC);PayloadTypeRegistry.clientboundPlay().register(Snapshot.TYPE,Snapshot.CODEC);registered=true;}
    private static void text(String s,int bound){if(s==null||s.length()>bound)throw new IllegalArgumentException("Invalid overview text");}
    private static void identifier(String s){text(s,160);if(!s.isEmpty()&&!s.matches("[A-Za-z0-9_:/|.-]+"))throw new IllegalArgumentException("Invalid overview ID");}
    private static boolean ratio(double v){return Double.isFinite(v)&&v>=0&&v<=1;}
    private static void asset(String s){text(s,180);if(!s.isEmpty()&&!s.matches("[a-z0-9_/-]+"))throw new IllegalArgumentException("Invalid overview asset");}
}
