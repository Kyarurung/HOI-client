package dev.hoi.protocol.commander;

import com.google.gson.Gson;
import dev.hoi.protocol.ProtocolJsonCodec;
import java.util.*;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public final class CommanderAirWingPayloads {
    private static final Gson JSON=new Gson();
    private static boolean registered;
    private CommanderAirWingPayloads() {}
    public enum Action { OPEN, CLOSE }
    public record Request(String token,Action action,String wing) implements CustomPacketPayload {
        public Request {UUID.fromString(token);Objects.requireNonNull(action);identifier(wing);if(wing.isEmpty())throw new IllegalArgumentException("Missing air wing");}
        public static final Type<Request> TYPE=new Type<>(Identifier.parse("hoi:commander_air_wing_request_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Request> CODEC=StreamCodec.of((b,p)->{b.writeUtf(p.token,36);b.writeEnum(p.action);b.writeUtf(p.wing,160);},b->new Request(b.readUtf(36),b.readEnum(Action.class),b.readUtf(160)));
        @Override public Type<Request> type(){return TYPE;}
    }
    public record Equipment(String name,String type,String icon,boolean carrier,int count) {
        public Equipment {text(name,160);text(type,160);asset(icon);if(count<0||count>100000)throw new IllegalArgumentException("Invalid air wing equipment");}
    }
    public record View(String token,String wing,String country,String name,String icon,boolean carrier,String equipmentName,String manpower,String ratio,List<String> missions,
            List<CommanderShipPayloads.Column> columns,List<Equipment> equipment,String ace,String aceIcon,Map<String,String> tooltips,Map<String,String> labels,String error) {
        public View {UUID.fromString(token);identifier(wing);identifier(country);text(name,160);asset(icon);text(equipmentName,160);text(manpower,32);text(ratio,32);text(ace,160);asset(aceIcon);text(error,400);
            missions=List.copyOf(missions);columns=List.copyOf(columns);equipment=List.copyOf(equipment);tooltips=Map.copyOf(tooltips);labels=Map.copyOf(labels);
            if(missions.size()>12||columns.size()>3||equipment.size()>32||tooltips.size()>32||labels.size()>32)throw new IllegalArgumentException("Invalid air wing view");
            missions.forEach(CommanderAirWingPayloads::identifier);labels.forEach((key,value)->{identifier(key);text(value,1000);});tooltips.forEach((key,value)->{identifier(key);text(value,3000);});}
        public String label(String key){return labels.getOrDefault(key,"");}
        public String tooltip(String key){return tooltips.getOrDefault(key,"");}
    }
    public record Snapshot(String json) implements CustomPacketPayload {
        public Snapshot {text(json,65536);if(json.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>65536)throw new IllegalArgumentException("Air wing packet exceeds byte budget");}
        public static final Type<Snapshot> TYPE=new Type<>(Identifier.parse("hoi:commander_air_wing_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Snapshot> CODEC=StreamCodec.of((b,p)->b.writeUtf(p.json,65536),b->new Snapshot(b.readUtf(65536)));
        public static Snapshot of(View view){return new Snapshot(JSON.toJson(view));}
        public View view(){return ProtocolJsonCodec.read(JSON,json,View.class);}
        @Override public Type<Snapshot> type(){return TYPE;}
    }
    public static synchronized void registerPayloadTypes(){if(registered)return;PayloadTypeRegistry.serverboundPlay().register(Request.TYPE,Request.CODEC);PayloadTypeRegistry.clientboundPlay().register(Snapshot.TYPE,Snapshot.CODEC);registered=true;}
    private static void text(String s,int bound){if(s==null||s.length()>bound)throw new IllegalArgumentException("Invalid air wing text");}
    private static void identifier(String s){text(s,160);if(!s.isEmpty()&&!s.matches("[A-Za-z0-9_:/|.-]+"))throw new IllegalArgumentException("Invalid air wing ID");}
    private static void asset(String s){text(s,180);if(!s.isEmpty()&&!s.matches("[a-z0-9_/-]+"))throw new IllegalArgumentException("Invalid air wing asset");}
}
