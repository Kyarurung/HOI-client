package dev.hoi.protocol.commander;

import com.google.gson.Gson;
import dev.hoi.protocol.ProtocolJsonCodec;
import java.util.*;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public final class CommanderBattlePayloads {
    private static final Gson JSON=new Gson();
    private static boolean registered;
    public static final int MARKERS=128,ROWS=128,ICONS=32,BYTES=400000;
    public static final List<String> LANES=List.of("outside","carrier","battle","screen","visible","hidden","sunk");
    private CommanderBattlePayloads() {}
    public enum Kind { LAND, NAVAL, RESULT }
    public enum Action { OPEN, CLOSE, DISENGAGE, DISMISS, DISMISS_ALL }
    public enum Tone { BALANCED, WINNING, LOSING }
    public record Request(String token,Action action,Kind kind,String id) implements CustomPacketPayload {
        public Request {UUID.fromString(token);Objects.requireNonNull(action);Objects.requireNonNull(kind);identifier(id);}
        public static final Type<Request> TYPE=new Type<>(Identifier.parse("hoi:commander_battle_request_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Request> CODEC=StreamCodec.of((b,p)->{b.writeUtf(p.token,36);b.writeEnum(p.action);b.writeEnum(p.kind);b.writeUtf(p.id,160);},
                b->new Request(b.readUtf(36),b.readEnum(Action.class),b.readEnum(Kind.class),b.readUtf(160)));
        @Override public Type<Request> type(){return TYPE;}
    }
    public record Marker(String id,Kind kind,double x,double y,double z,double headingX,double headingZ,String value,Tone tone,boolean urban,int frame,String tooltip) {
        public Marker {
            identifier(id);Objects.requireNonNull(kind);Objects.requireNonNull(tone);text(value,16);text(tooltip,4000);
            for(double n:new double[]{x,y,z})if(!Double.isFinite(n)||Math.abs(n)>30_000_000)throw new IllegalArgumentException("Invalid battle position");
            for(double n:new double[]{headingX,headingZ})if(!Double.isFinite(n)||Math.abs(n)>1.001)throw new IllegalArgumentException("Invalid battle heading");
            if(frame<0||frame>15)throw new IllegalArgumentException("Invalid battle frame");
        }
    }
    public record Markers(String dimension,List<Marker> markers) implements CustomPacketPayload {
        public Markers {
            text(dimension,128);markers=List.copyOf(markers);
            if(Identifier.tryParse(dimension)==null||markers.size()>MARKERS||markers.stream().map(marker->marker.kind()+"|"+marker.id()).distinct().count()!=markers.size())throw new IllegalArgumentException("Invalid battle markers");
        }
        public static final Type<Markers> TYPE=new Type<>(Identifier.parse("hoi:commander_battle_markers_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Markers> CODEC=StreamCodec.of((b,p)->{
            b.writeUtf(p.dimension,128);b.writeVarInt(p.markers.size());
            for(var m:p.markers) {
                b.writeUtf(m.id,160);b.writeEnum(m.kind);b.writeDouble(m.x);b.writeDouble(m.y);b.writeDouble(m.z);b.writeFloat((float)m.headingX);b.writeFloat((float)m.headingZ);
                b.writeUtf(m.value,16);b.writeEnum(m.tone);b.writeBoolean(m.urban);b.writeByte(m.frame);b.writeUtf(m.tooltip,4000);
            }
        },b->{
            String dimension=b.readUtf(128);int count=b.readVarInt();
            if(count<0||count>MARKERS)throw new IllegalArgumentException("Too many battle markers");
            var markers=new ArrayList<Marker>(count);
            for(int i=0;i<count;i++)markers.add(new Marker(b.readUtf(160),b.readEnum(Kind.class),b.readDouble(),b.readDouble(),b.readDouble(),b.readFloat(),b.readFloat(),b.readUtf(16),b.readEnum(Tone.class),b.readBoolean(),b.readByte(),b.readUtf(4000)));
            return new Markers(dimension,markers);
        });
        @Override public Type<Markers> type(){return TYPE;}
    }
    public record Icon(String image,String tooltip) {
        public Icon {asset(image);text(tooltip,3000);}
    }
    public record Division(String id,String name,String flag,String icon,boolean own,double organization,double strength,String soft,String hard,String guard,List<Icon> modifiers,Map<String,String> tooltips) {
        public Division {
            identifier(id);text(name,160);identifier(flag);asset(icon);ratio(organization);ratio(strength);for(String value:List.of(soft,hard,guard))text(value,16);
            modifiers=icons(modifiers);tooltips=texts(tooltips);
        }
        public String tooltip(String key){return tooltips.getOrDefault(key,"");}
    }
    public record Air(String count,String text,String tooltip) {
        public Air {CommanderBattlePayloads.text(count,16);CommanderBattlePayloads.text(text,400);CommanderBattlePayloads.text(tooltip,2000);}
    }
    public record Tactic(String image,String tooltip,boolean countered) {
        public Tactic {asset(image);text(tooltip,3000);}
    }
    public record Side(String country,String countryTooltip,String leader,String portrait,String leaderTooltip,String skill,String skillTooltip,List<Icon> traits,boolean initiative,String headquarters,Tactic tactic,String width,List<Icon> modifiers,Air air,
            String committedLabel,String committedTooltip,List<Division> committed,String reservesLabel,List<Division> reserves) {
        public Side {
            identifier(country);text(countryTooltip,600);text(leader,160);asset(portrait);text(leaderTooltip,4000);text(skill,16);text(skillTooltip,600);text(headquarters,600);Objects.requireNonNull(tactic);text(width,16);
            traits=icons(traits);modifiers=icons(modifiers);text(committedLabel,80);text(committedTooltip,600);text(reservesLabel,80);committed=List.copyOf(committed);reserves=List.copyOf(reserves);
            if(committed.size()>ROWS||reserves.size()>ROWS)throw new IllegalArgumentException("Too many battle divisions");
        }
    }
    public record TacticRow(String id,String name,String image,boolean attacking,boolean unlocked,String phase,List<String> effects,String tooltip) {
        public TacticRow {identifier(id);text(name,160);asset(image);text(phase,160);effects=List.copyOf(effects);effects.forEach(value->text(value,32));text(tooltip,3000);if(effects.size()>6)throw new IllegalArgumentException("Invalid tactic effects");}
    }
    public record Land(String token,String id,String title,String terrain,String terrainTooltip,double progress,int heading,String width,Side attacker,Side defender,String phase,List<TacticRow> tactics,Map<String,String> labels,String error) {
        public Land {
            UUID.fromString(token);identifier(id);text(title,160);asset(terrain);text(terrainTooltip,3000);ratio(progress);text(width,16);text(phase,200);tactics=List.copyOf(tactics);labels=texts(labels);text(error,400);
            if(heading<0||heading>4||tactics.size()>ROWS||error.isEmpty()&&(attacker==null||defender==null))throw new IllegalArgumentException("Invalid land battle");
        }
        public Side side(boolean attacking){return attacking?attacker:defender;}
        public String label(String key){return labels.getOrDefault(key,"");}
    }
    public record Ships(String image,String flag,int count,double organization,double strength,boolean hidden,String tooltip) {
        public Ships {asset(image);identifier(flag);ratio(organization);ratio(strength);text(tooltip,3000);if(count<1)throw new IllegalArgumentException("Invalid battle ships");}
    }
    public record Fleet(String country,String countryTooltip,String leader,String portrait,String leaderTooltip,List<String> skills,List<Icon> traits,Map<String,String> stats,Map<String,String> statTooltips,String capitalScreening,String carrierScreening,Map<String,List<Ships>> lanes) {
        public Fleet {
            identifier(country);text(countryTooltip,600);text(leader,160);asset(portrait);text(leaderTooltip,4000);skills=List.copyOf(skills);skills.forEach(value->text(value,8));traits=icons(traits);stats=texts(stats);statTooltips=texts(statTooltips);
            text(capitalScreening,16);text(carrierScreening,16);
            var copy=new TreeMap<String,List<Ships>>();lanes.forEach((lane,ships)->{if(!LANES.contains(lane)||ships.size()>ROWS)throw new IllegalArgumentException("Invalid battle lane");copy.put(lane,List.copyOf(ships));});lanes=copy;
            if(!skills.isEmpty()&&skills.size()!=4)throw new IllegalArgumentException("Invalid admiral skills");
        }
        public List<Ships> lane(String lane){return lanes.getOrDefault(lane,List.of());}
    }
    public record Naval(String token,String id,String title,String weather,String conditions,double progress,boolean attacking,boolean disengage,String disengageTooltip,Fleet attacker,Fleet defender,Map<String,String> labels,String error) {
        public Naval {
            UUID.fromString(token);identifier(id);text(title,160);asset(weather);text(conditions,3000);ratio(progress);text(disengageTooltip,600);labels=texts(labels);text(error,400);
            if(error.isEmpty()&&(attacker==null||defender==null))throw new IllegalArgumentException("Invalid naval battle");
        }
        public Fleet fleet(boolean attacking){return attacking?attacker:defender;}
        public String label(String key){return labels.getOrDefault(key,"");}
    }
    public record Survivor(String image,int count,List<String> flags,String tooltip) {
        public Survivor {asset(image);flags=List.copyOf(flags);flags.forEach(CommanderBattlePayloads::identifier);text(tooltip,3000);if(count<0||flags.size()>16)throw new IllegalArgumentException("Invalid battle survivors");}
    }
    public record Loss(String flag,int count,String tooltip) {
        public Loss {identifier(flag);text(tooltip,2000);if(count<0)throw new IllegalArgumentException("Invalid battle loss");}
    }
    public record Lost(String image,String tooltip,List<Loss> losses) {
        public Lost {asset(image);text(tooltip,600);losses=List.copyOf(losses);if(losses.size()>16)throw new IllegalArgumentException("Invalid battle losses");}
    }
    public record ResultSide(String country,String countryTooltip,String leader,String portrait,String leaderTooltip,List<Icon> traits,List<Survivor> survivors,List<Lost> lost,String details) {
        public ResultSide {
            identifier(country);text(countryTooltip,600);text(leader,160);asset(portrait);text(leaderTooltip,4000);traits=icons(traits);survivors=List.copyOf(survivors);lost=List.copyOf(lost);text(details,8000);
            if(survivors.size()>64||lost.size()>64)throw new IllegalArgumentException("Invalid naval result");
        }
    }
    public record Result(String token,String id,String title,String outcome,ResultSide attacker,ResultSide defender,Map<String,String> labels,String error) {
        public Result {UUID.fromString(token);identifier(id);text(title,160);text(outcome,80);labels=texts(labels);text(error,400);if(error.isEmpty()&&(attacker==null||defender==null))throw new IllegalArgumentException("Invalid naval result");}
        public String label(String key){return labels.getOrDefault(key,"");}
    }
    public record Snapshot(Kind kind,String json) implements CustomPacketPayload {
        public Snapshot {Objects.requireNonNull(kind);text(json,BYTES);if(json.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>BYTES)throw new IllegalArgumentException("Battle packet exceeds byte budget");}
        public static final Type<Snapshot> TYPE=new Type<>(Identifier.parse("hoi:commander_battle_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Snapshot> CODEC=StreamCodec.of((b,p)->{b.writeEnum(p.kind);b.writeUtf(p.json,BYTES);},b->new Snapshot(b.readEnum(Kind.class),b.readUtf(BYTES)));
        public static Snapshot of(Land land){return new Snapshot(Kind.LAND,JSON.toJson(land));}
        public static Snapshot of(Naval naval){return new Snapshot(Kind.NAVAL,JSON.toJson(naval));}
        public static Snapshot of(Result result){return new Snapshot(Kind.RESULT,JSON.toJson(result));}
        public Land land(){return ProtocolJsonCodec.read(JSON,json,Land.class);}
        public Naval naval(){return ProtocolJsonCodec.read(JSON,json,Naval.class);}
        public Result result(){return ProtocolJsonCodec.read(JSON,json,Result.class);}
        @Override public Type<Snapshot> type(){return TYPE;}
    }
    public static synchronized void registerPayloadTypes(){
        if(registered)return;
        PayloadTypeRegistry.serverboundPlay().register(Request.TYPE,Request.CODEC);PayloadTypeRegistry.clientboundPlay().register(Snapshot.TYPE,Snapshot.CODEC);PayloadTypeRegistry.clientboundPlay().register(Markers.TYPE,Markers.CODEC);registered=true;
    }
    private static Map<String,String> texts(Map<String,String> values){
        if(values.size()>64)throw new IllegalArgumentException("Invalid battle labels");
        values.forEach((key,value)->{identifier(key);text(value,3000);});
        return Map.copyOf(values);
    }
    private static List<Icon> icons(List<Icon> values){if(values.size()>ICONS)throw new IllegalArgumentException("Too many battle icons");return List.copyOf(values);}
    private static void ratio(double n){if(!Double.isFinite(n)||n<0||n>1)throw new IllegalArgumentException("Invalid battle ratio");}
    private static void text(String s,int bound){if(s==null||s.length()>bound)throw new IllegalArgumentException("Invalid battle text");}
    private static void identifier(String s){text(s,160);if(!s.isEmpty()&&!s.matches("[A-Za-z0-9_:/|.-]+"))throw new IllegalArgumentException("Invalid battle ID");}
    private static void asset(String s){text(s,180);if(!s.isEmpty()&&!s.matches("[a-z0-9_/-]+"))throw new IllegalArgumentException("Invalid battle asset");}
}
