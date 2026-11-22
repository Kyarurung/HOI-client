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

public final class CommanderDivisionPayloads {
    private static final Gson JSON=new Gson();
    private static boolean registered;
    private CommanderDivisionPayloads() {}
    public static final int NAME_LIMIT=64;
    public enum Action { OPEN, CLOSE, RENAME }
    public record Request(String token,Action action,String division,String name) implements CustomPacketPayload {
        public Request(String token,Action action,String division) {this(token,action,division,"");}
        public Request {UUID.fromString(token);Objects.requireNonNull(action);identifier(division);text(name,NAME_LIMIT);if(division.isEmpty())throw new IllegalArgumentException("Missing division");}
        public static final Type<Request> TYPE=new Type<>(Identifier.parse("hoi:commander_division_request_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Request> CODEC=StreamCodec.of((b,p)->{b.writeUtf(p.token,36);b.writeEnum(p.action);b.writeUtf(p.division,160);b.writeUtf(p.name,NAME_LIMIT);},b->new Request(b.readUtf(36),b.readEnum(Action.class),b.readUtf(160),b.readUtf(NAME_LIMIT)));
        @Override public Type<Request> type(){return TYPE;}
    }
    public record Stat(String id,String group,double value,String unit,String tooltip) {
        public Stat {identifier(id);identifier(group);identifier(unit);text(tooltip,4000);if(!Double.isFinite(value))throw new IllegalArgumentException("Invalid division stat");}
    }
    public record Variant(String name,String icon,long amount,String source) {
        public Variant {text(name,160);asset(icon);text(source,160);if(amount<0)throw new IllegalArgumentException("Invalid division equipment");}
    }
    public record Equipment(String name,long have,long need,List<Variant> variants) {
        public Equipment {text(name,160);variants=List.copyOf(variants);if(have<0||need<0||variants.size()>16)throw new IllegalArgumentException("Invalid division equipment");}
    }
    public record Officer(String title,String name,String portrait,boolean promotable,String promotion) {
        public Officer {text(title,64);text(name,160);asset(portrait);text(promotion,1200);}
    }
    public record Unit(String id,String name,String icon,String tooltip,List<IndustryTankDesigner.Adjustment> terrains) {
        public Unit {identifier(id);text(name,160);asset(icon);text(tooltip,2000);terrains=List.copyOf(terrains);if(terrains.size()>16)throw new IllegalArgumentException("Invalid division unit");}
    }
    public record Template(String id,String name,String icon,int priority,String blocked) {
        public Template {identifier(id);text(name,160);asset(icon);text(blocked,400);if(priority<0||priority>2)throw new IllegalArgumentException("Invalid division template");}
    }
    public record View(String token,String division,String country,String name,boolean custom,String template,String templateId,boolean locked,int priority,List<Template> templates,Map<String,String> tooltips,String icon,boolean obsolete,int level,double experience,String manpower,String equipment,Officer officer,
            List<Stat> stats,List<Equipment> equipmentRows,List<List<Unit>> columns,List<Unit> support,int combatWidth,double hardness,List<IndustryTankDesigner.Adjustment> terrains,Map<String,String> labels,String error) {
        public View {UUID.fromString(token);identifier(division);identifier(country);text(name,160);text(template,160);identifier(templateId);templates=List.copyOf(templates);tooltips=Map.copyOf(tooltips);asset(icon);text(manpower,32);text(equipment,32);text(error,400);
            stats=List.copyOf(stats);equipmentRows=List.copyOf(equipmentRows);columns=columns.stream().map(List::copyOf).toList();support=List.copyOf(support);terrains=List.copyOf(terrains);labels=Map.copyOf(labels);
            if(priority<0||priority>2||templates.size()>256||tooltips.size()>32||level<0||level>4||!Double.isFinite(experience)||experience<0||experience>1||combatWidth<0||combatWidth>1000||!Double.isFinite(hardness)||hardness<0||hardness>1
                    ||stats.size()>64||equipmentRows.size()>64||columns.size()>8||columns.stream().anyMatch(c->c.size()>8)||support.size()>8||terrains.size()>16||labels.size()>48)throw new IllegalArgumentException("Invalid division view");
            labels.forEach((key,value)->{identifier(key);text(value,2000);});tooltips.forEach((key,value)->{identifier(key);text(value,4000);});}
        public String label(String key){return labels.getOrDefault(key,"");}
        public String tooltip(String key){return tooltips.getOrDefault(key,"");}
    }
    public record Snapshot(String json) implements CustomPacketPayload {
        public Snapshot {text(json,200000);if(json.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>200000)throw new IllegalArgumentException("Division packet exceeds byte budget");}
        public static final Type<Snapshot> TYPE=new Type<>(Identifier.parse("hoi:commander_division_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Snapshot> CODEC=StreamCodec.of((b,p)->b.writeUtf(p.json,200000),b->new Snapshot(b.readUtf(200000)));
        public static Snapshot of(View view){return new Snapshot(JSON.toJson(view));}
        public View view(){return ProtocolJsonCodec.read(JSON,json,View.class);}
        @Override public Type<Snapshot> type(){return TYPE;}
    }
    public static synchronized void registerPayloadTypes(){if(registered)return;PayloadTypeRegistry.serverboundPlay().register(Request.TYPE,Request.CODEC);PayloadTypeRegistry.clientboundPlay().register(Snapshot.TYPE,Snapshot.CODEC);registered=true;}
    private static void text(String s,int bound){if(s==null||s.length()>bound)throw new IllegalArgumentException("Invalid division text");}
    private static void identifier(String s){text(s,160);if(!s.isEmpty()&&!s.matches("[A-Za-z0-9_:/|.-]+"))throw new IllegalArgumentException("Invalid division ID");}
    private static void asset(String s){text(s,180);if(!s.isEmpty()&&!s.matches("[a-z0-9_/-]+"))throw new IllegalArgumentException("Invalid division asset");}
}
