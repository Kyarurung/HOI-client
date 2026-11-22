package dev.hoi.protocol.commander;

import com.google.gson.Gson;
import dev.hoi.protocol.ProtocolJsonCodec;
import java.util.*;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public final class CommanderRecordPayloads {
    private static final Gson JSON=new Gson();
    private static boolean registered;
    public static final int ROWS=24,MONTHS=60,FILTERS=64,BYTES=200000;
    private CommanderRecordPayloads() {}
    public enum Action { LOG, CLOSE_LOG, NAVY, CLOSE_NAVY, FOCUS, EQUIPMENT, CLOSE_EQUIPMENT }
    public record Request(String token,Action action,String target,int months,int offset,List<String> filters) implements CustomPacketPayload {
        public Request {
            UUID.fromString(token);Objects.requireNonNull(action);identifier(target);filters=List.copyOf(filters);filters.forEach(CommanderRecordPayloads::identifier);
            if(months<0||months>MONTHS||offset<0||offset>65535||filters.size()>FILTERS)throw new IllegalArgumentException("Invalid record request");
        }
        public static final Type<Request> TYPE=new Type<>(Identifier.parse("hoi:commander_record_request_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Request> CODEC=StreamCodec.of((b,p)->{
            b.writeUtf(p.token,36);b.writeEnum(p.action);b.writeUtf(p.target,160);b.writeVarInt(p.months);b.writeVarInt(p.offset);b.writeVarInt(p.filters.size());p.filters.forEach(s->b.writeUtf(s,160));
        },b->{
            String token=b.readUtf(36);var action=b.readEnum(Action.class);String target=b.readUtf(160);int months=b.readVarInt(),offset=b.readVarInt(),count=b.readVarInt();
            if(count<0||count>FILTERS)throw new IllegalArgumentException("Invalid record filters");
            var filters=new ArrayList<String>();for(int i=0;i<count;i++)filters.add(b.readUtf(160));
            return new Request(token,action,target,months,offset,filters);
        });
        @Override public Type<Request> type(){return TYPE;}
    }
    public record Party(String country,String countries,String portrait,String leader,String manpower,String armor,String air,String other) {
        public Party {identifier(country);text(countries,600);asset(portrait);text(leader,2000);for(String value:List.of(manpower,armor,air,other))text(value,80);}
    }
    public record Battle(String id,String province,String location,String locationTooltip,String date,String dateTooltip,String icon,String iconTooltip,String image,String imageTooltip,Party own,Party enemy) {
        public Battle {
            identifier(id);identifier(province);text(location,160);text(locationTooltip,400);text(date,80);text(dateTooltip,200);asset(icon);text(iconTooltip,400);asset(image);text(imageTooltip,4000);
            Objects.requireNonNull(own);Objects.requireNonNull(enemy);
        }
    }
    public record Loss(String name,String icon,String combat,String combatTooltip,String attrition,String attritionTooltip,String recovered,String recoveredTooltip) {
        public Loss {text(name,160);asset(icon);for(String value:List.of(combat,attrition,recovered))text(value,32);for(String value:List.of(combatTooltip,attritionTooltip,recoveredTooltip))text(value,600);}
    }
    public record Template(String id,String name,String icon,String ratio,String tooltip,String total) {
        public Template {identifier(id);text(name,160);asset(icon);text(ratio,32);text(tooltip,400);text(total,32);}
    }
    public record Log(String token,String theater,int months,int maximum,Set<String> excluded,String period,String manpower,String manpowerTooltip,String ratio,int offset,int total,List<Battle> battles,
            Loss lost,List<Loss> losses,List<Loss> detailed,Loss enemyLost,List<Loss> enemy,List<Loss> enemyDetailed,List<Template> templates,Map<String,String> labels,String error) {
        public Log {
            UUID.fromString(token);identifier(theater);excluded=Set.copyOf(excluded);battles=List.copyOf(battles);losses=List.copyOf(losses);detailed=List.copyOf(detailed);enemy=List.copyOf(enemy);enemyDetailed=List.copyOf(enemyDetailed);templates=List.copyOf(templates);
            text(period,200);text(manpower,200);text(manpowerTooltip,600);text(ratio,200);text(error,400);labels=texts(labels);excluded.forEach(CommanderRecordPayloads::identifier);
            if(months<0||months>MONTHS||maximum<0||maximum>MONTHS||offset<0||total<0||battles.size()>ROWS||losses.size()>256||detailed.size()>256||enemy.size()>256||enemyDetailed.size()>256||templates.size()>256||excluded.size()>FILTERS)throw new IllegalArgumentException("Invalid combat log");
        }
        public String label(String key){return labels.getOrDefault(key,"");}
    }
    public record Kill(String country,String count) {
        public Kill {identifier(country);text(count,16);}
    }
    public record Hull(String key,String name,String image,int lost,List<Kill> kills,String tooltip) {
        public Hull {identifier(key);text(name,160);asset(image);kills=List.copyOf(kills);text(tooltip,600);if(lost<0||kills.size()>3)throw new IllegalArgumentException("Invalid hull losses");}
    }
    public record Accident(String name,String image,String trainingSunk,String minesSunk,String trainingDamage,String minesDamage,String tooltip) {
        public Accident {text(name,160);asset(image);for(String value:List.of(trainingSunk,minesSunk,trainingDamage,minesDamage))text(value,32);text(tooltip,800);}
    }
    public record Section(String id,String title,int lost,int sunk,List<Hull> hulls,List<Accident> accidents) {
        public Section {identifier(id);text(title,160);hulls=List.copyOf(hulls);accidents=List.copyOf(accidents);if(lost<0||sunk<0||hulls.size()>64||accidents.size()>64)throw new IllegalArgumentException("Invalid loss section");}
    }
    public record Ship(String name,String image,String owner,String killer,String tooltip) {
        public Ship {text(name,160);asset(image);identifier(owner);identifier(killer);text(tooltip,600);}
    }
    public record Choice(String id,String name) {
        public Choice {identifier(id);text(name,160);}
    }
    public record Losses(String token,String target,String viewer,int lost,int sunk,String country,String faction,List<Choice> countries,List<Choice> factions,List<Section> battles,List<Section> periods,List<Ship> ships,Map<String,String> labels,String error) {
        public Losses {
            UUID.fromString(token);identifier(target);identifier(viewer);identifier(country);identifier(faction);countries=List.copyOf(countries);factions=List.copyOf(factions);battles=List.copyOf(battles);periods=List.copyOf(periods);ships=List.copyOf(ships);labels=texts(labels);text(error,400);
            if(lost<0||sunk<0||countries.size()>128||factions.size()>64||battles.size()>128||periods.size()>8||ships.size()>256)throw new IllegalArgumentException("Invalid naval losses");
        }
        public String label(String key){return labels.getOrDefault(key,"");}
    }
    public record Total(String icon,String value,String tooltip) {
        public Total {asset(icon);text(value,64);text(tooltip,800);}
    }
    public record Item(String name,String image,String amount) {
        public Item {text(name,160);asset(image);text(amount,32);}
    }
    public record Archetype(String id,String name,String amount,boolean carrier,List<Item> items) {
        public Archetype {identifier(id);text(name,160);text(amount,64);items=List.copyOf(items);if(items.size()>64)throw new IllegalArgumentException("Invalid equipment archetype");}
    }
    public record FieldEquipment(String token,String target,List<Total> totals,List<Archetype> rows,Map<String,String> labels,String error) {
        public FieldEquipment {
            UUID.fromString(token);identifier(target);totals=List.copyOf(totals);rows=List.copyOf(rows);labels=texts(labels);text(error,400);
            if(totals.size()>3||rows.size()>128)throw new IllegalArgumentException("Invalid field equipment");
        }
        public String label(String key){return labels.getOrDefault(key,"");}
    }
    public record LogSnapshot(String json) implements CustomPacketPayload {
        public LogSnapshot {bounded(json);}
        public static final Type<LogSnapshot> TYPE=new Type<>(Identifier.parse("hoi:commander_combat_log_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf,LogSnapshot> CODEC=StreamCodec.of((b,p)->b.writeUtf(p.json,BYTES),b->new LogSnapshot(b.readUtf(BYTES)));
        public static LogSnapshot of(Log log){return new LogSnapshot(JSON.toJson(log));}
        public Log log(){return ProtocolJsonCodec.read(JSON,json,Log.class);}
        @Override public Type<LogSnapshot> type(){return TYPE;}
    }
    public record LossesSnapshot(String json) implements CustomPacketPayload {
        public LossesSnapshot {bounded(json);}
        public static final Type<LossesSnapshot> TYPE=new Type<>(Identifier.parse("hoi:commander_naval_losses_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf,LossesSnapshot> CODEC=StreamCodec.of((b,p)->b.writeUtf(p.json,BYTES),b->new LossesSnapshot(b.readUtf(BYTES)));
        public static LossesSnapshot of(Losses losses){return new LossesSnapshot(JSON.toJson(losses));}
        public Losses losses(){return ProtocolJsonCodec.read(JSON,json,Losses.class);}
        @Override public Type<LossesSnapshot> type(){return TYPE;}
    }
    public record EquipmentSnapshot(String json) implements CustomPacketPayload {
        public EquipmentSnapshot {bounded(json);}
        public static final Type<EquipmentSnapshot> TYPE=new Type<>(Identifier.parse("hoi:commander_field_equipment_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf,EquipmentSnapshot> CODEC=StreamCodec.of((b,p)->b.writeUtf(p.json,BYTES),b->new EquipmentSnapshot(b.readUtf(BYTES)));
        public static EquipmentSnapshot of(FieldEquipment equipment){return new EquipmentSnapshot(JSON.toJson(equipment));}
        public FieldEquipment equipment(){return ProtocolJsonCodec.read(JSON,json,FieldEquipment.class);}
        @Override public Type<EquipmentSnapshot> type(){return TYPE;}
    }
    public static synchronized void registerPayloadTypes(){
        if(registered)return;
        PayloadTypeRegistry.serverboundPlay().register(Request.TYPE,Request.CODEC);PayloadTypeRegistry.clientboundPlay().register(LogSnapshot.TYPE,LogSnapshot.CODEC);PayloadTypeRegistry.clientboundPlay().register(LossesSnapshot.TYPE,LossesSnapshot.CODEC);PayloadTypeRegistry.clientboundPlay().register(EquipmentSnapshot.TYPE,EquipmentSnapshot.CODEC);registered=true;
    }
    private static Map<String,String> texts(Map<String,String> labels){
        if(labels.size()>48)throw new IllegalArgumentException("Invalid record labels");
        labels.forEach((key,value)->{identifier(key);text(value,1000);});
        return Map.copyOf(labels);
    }
    private static void bounded(String json){text(json,BYTES);if(json.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>BYTES)throw new IllegalArgumentException("Record packet exceeds byte budget");}
    private static void text(String s,int bound){if(s==null||s.length()>bound)throw new IllegalArgumentException("Invalid record text");}
    private static void identifier(String s){text(s,160);if(!s.isEmpty()&&!s.matches("[A-Za-z0-9_:/|.-]+"))throw new IllegalArgumentException("Invalid record ID");}
    private static void asset(String s){text(s,180);if(!s.isEmpty()&&!s.matches("[a-z0-9_/-]+"))throw new IllegalArgumentException("Invalid record asset");}
}
