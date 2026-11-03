package dev.hoi.protocol;

import java.util.*;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public final class ProtocolCommanderOrders {
    private static boolean registered;
    private ProtocolCommanderOrders() {}
    public record Context(String token,long revision,String country,String dimension,String pending) implements CustomPacketPayload {
        public Context {UUID.fromString(token);if(revision<0||!country.matches("[A-Z0-9]{3}")||dimension.length()>128||pending.length()>160)throw new IllegalArgumentException("Invalid command context");}
        public static final Type<Context> TYPE=new Type<>(Identifier.parse("hoi:commander_context_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Context> CODEC=StreamCodec.of((b,p)->{b.writeUtf(p.token,36);b.writeVarLong(p.revision);b.writeUtf(p.country,3);b.writeUtf(p.dimension,128);b.writeUtf(p.pending,160);},b->new Context(b.readUtf(36),b.readVarLong(),b.readUtf(3),b.readUtf(128),b.readUtf(160)));
        @Override public Type<Context> type(){return TYPE;}
    }
    public record Command(String token,long revision,String action,List<String> selection,List<ProtocolCommanderHud.Ray> rays) implements CustomPacketPayload {
        public Command {UUID.fromString(token);if(revision<0||!action.matches("[A-Z_/]{1,80}"))throw new IllegalArgumentException("Invalid direct command");selection=keys(selection);rays=List.copyOf(rays);if(rays.size()>64)throw new IllegalArgumentException("Too many command rays");}
        public static final Type<Command> TYPE=new Type<>(Identifier.parse("hoi:commander_command_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Command> CODEC=StreamCodec.of((b,p)->{b.writeUtf(p.token,36);b.writeVarLong(p.revision);b.writeUtf(p.action,80);writeKeys(b,p.selection);b.writeVarInt(p.rays.size());for(var r:p.rays){b.writeDouble(r.x());b.writeDouble(r.y());b.writeDouble(r.z());}},b->{String token=b.readUtf(36);long revision=b.readVarLong();String action=b.readUtf(80);var keys=readKeys(b);int n=b.readVarInt();if(n<0||n>64)throw new IllegalArgumentException("Invalid command rays");var rays=new ArrayList<ProtocolCommanderHud.Ray>();for(int i=0;i<n;i++)rays.add(new ProtocolCommanderHud.Ray(b.readDouble(),b.readDouble(),b.readDouble()));return new Command(token,revision,action,keys,rays);});
        @Override public Type<Command> type(){return TYPE;}
    }
    public record Selection(String token,long sequence,List<String> keys) implements CustomPacketPayload {
        public Selection {UUID.fromString(token);if(sequence<0)throw new IllegalArgumentException("Invalid selection sequence");keys=ProtocolCommanderOrders.keys(keys);}
        public static final Type<Selection> TYPE=new Type<>(Identifier.parse("hoi:commander_selection_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Selection> CODEC=StreamCodec.of((b,p)->{b.writeUtf(p.token,36);b.writeVarLong(p.sequence);writeKeys(b,p.keys);},b->new Selection(b.readUtf(36),b.readVarLong(),readKeys(b)));
        @Override public Type<Selection> type(){return TYPE;}
    }
    public record DetailPage(String token,long sequence,long generation,int offset,int total,List<ProtocolCommanderHud.Member> members) {
        public DetailPage {UUID.fromString(token);members=List.copyOf(members);if(sequence<0||generation<0||offset<0||offset%32!=0||total<0||total>2048||offset>total||members.size()!=Math.min(32,total-offset)||members.stream().map(ProtocolCommanderHud.Member::id).distinct().count()!=members.size()||members.stream().anyMatch(m->m.request().mode()!=ProtocolCommanderHud.Mode.ARMY_UNITS))throw new IllegalArgumentException("Invalid selected division page");}
    }
    public record Details(String json) implements CustomPacketPayload {
        private static final com.google.gson.Gson JSON=new com.google.gson.Gson();
        public Details {if(json==null||json.length()>1000000||json.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>1000000)throw new IllegalArgumentException("Selected division page exceeds byte budget");}
        public static Details of(DetailPage page){return new Details(JSON.toJson(page));}
        public DetailPage page(){return ProtocolJsonCodec.read(JSON,json,DetailPage.class);}
        public static final Type<Details> TYPE=new Type<>(Identifier.parse("hoi:commander_selection_details_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Details> CODEC=StreamCodec.of((b,p)->b.writeUtf(p.json,1000000),b->new Details(b.readUtf(1000000)));
        @Override public Type<Details> type(){return TYPE;}
    }
    public record Check(String token,long revision,long sequence,long request,List<String> armies) implements CustomPacketPayload {
        public Check {UUID.fromString(token);armies=keys(armies);if(revision<0||sequence<0||request<0||armies.size()>160)throw new IllegalArgumentException("Invalid command availability query");}
        public static final Type<Check> TYPE=new Type<>(Identifier.parse("hoi:commander_availability_query_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Check> CODEC=StreamCodec.of((b,p)->{b.writeUtf(p.token,36);b.writeVarLong(p.revision);b.writeVarLong(p.sequence);b.writeVarLong(p.request);writeKeys(b,p.armies);},b->new Check(b.readUtf(36),b.readVarLong(),b.readVarLong(),b.readVarLong(),readKeys(b)));
        @Override public Type<Check> type(){return TYPE;}
    }
    public record Decision(boolean enabled,boolean target,String reason,String details,Boolean warning) {
        public Decision(boolean enabled,boolean target,String reason){this(enabled,target,reason,"");}
        public Decision(boolean enabled,boolean target,String reason,String details){this(enabled,target,reason,details,null);}
        public Decision {details=details==null?"":details;if(details.length()>3000)throw new IllegalArgumentException("Command detail too long");Objects.requireNonNull(reason);if(reason.length()>400||enabled!=reason.isEmpty()||!enabled&&target)throw new IllegalArgumentException("Invalid command availability");}
    }
    public record Assessment(String token,long revision,long sequence,long request,Map<String,Decision> actions) {
        public Assessment {UUID.fromString(token);actions=Map.copyOf(actions);if(revision<0||sequence<0||request<0||actions.size()>64||actions.keySet().stream().anyMatch(a->!a.matches("[A-Z_/]{1,80}")))throw new IllegalArgumentException("Invalid command assessment");}
    }
    public record Availability(String json) implements CustomPacketPayload {
        private static final com.google.gson.Gson JSON=new com.google.gson.Gson();
        public Availability {if(json==null||json.length()>262144||json.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>262144)throw new IllegalArgumentException("Command assessment exceeds byte budget");}
        public static Availability of(Assessment value){return new Availability(JSON.toJson(value));}
        public Assessment assessment(){return ProtocolJsonCodec.read(JSON,json,Assessment.class);}
        public static final Type<Availability> TYPE=new Type<>(Identifier.parse("hoi:commander_availability_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Availability> CODEC=StreamCodec.of((b,p)->b.writeUtf(p.json,262144),b->new Availability(b.readUtf(262144)));
        @Override public Type<Availability> type(){return TYPE;}
    }
    private static List<String> keys(List<String> keys){var copy=List.copyOf(keys);if(copy.size()>2048||new HashSet<>(copy).size()!=copy.size()||copy.stream().anyMatch(k->!k.matches("[A-Za-z0-9_:/|.-]{1,160}")))throw new IllegalArgumentException("Invalid command selection");return copy;}
    private static void writeKeys(RegistryFriendlyByteBuf b,List<String> keys){b.writeVarInt(keys.size());keys.forEach(k->b.writeUtf(k,160));}
    private static List<String> readKeys(RegistryFriendlyByteBuf b){int n=b.readVarInt();if(n<0||n>2048)throw new IllegalArgumentException("Invalid command selection count");var keys=new ArrayList<String>();for(int i=0;i<n;i++)keys.add(b.readUtf(160));return keys;}
    public static synchronized void registerPayloadTypes(){if(registered)return;ProtocolNavalInvasion.registerPayloadTypes();PayloadTypeRegistry.clientboundPlay().register(Context.TYPE,Context.CODEC);PayloadTypeRegistry.clientboundPlay().register(Details.TYPE,Details.CODEC);PayloadTypeRegistry.clientboundPlay().register(Availability.TYPE,Availability.CODEC);PayloadTypeRegistry.serverboundPlay().registerLarge(Check.TYPE,Check.CODEC,65536);PayloadTypeRegistry.serverboundPlay().registerLarge(Command.TYPE,Command.CODEC,400000);PayloadTypeRegistry.serverboundPlay().registerLarge(Selection.TYPE,Selection.CODEC,400000);registered=true;}
}
