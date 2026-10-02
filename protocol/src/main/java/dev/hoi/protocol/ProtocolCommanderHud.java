package dev.hoi.protocol;

import com.google.gson.Gson;
import java.util.*;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public final class ProtocolCommanderHud {
    private static final Gson JSON=new Gson();
    private static boolean registered;
    private ProtocolCommanderHud() {}
    public enum Mode { ARMY, FIELD_OFFICERS, NAVY, NAVY_HQ, AIR, ASSIGN_ARMY, ASSIGN_MARSHAL, PROMOTE, CREATE_ARMY, MOVE_DIVISIONS, NAVAL_TASK_FORCES, NAVAL_MISSION, AIR_MISSION, ARMY_PLANS, LAND_PLAN, ARMY_UNITS, PLAN_UNITS, ARMY_GROUPS, ARMY_GROUP, ARMY_GROUP_MEMBERS, ASSIGN_GROUP_MARSHAL, NAVY_FLEET, NAVY_FLEET_MEMBERS, NAVAL_ORDERS, AIR_ORDERS, NAVY_HEADQUARTERS, COMMANDER_TRAITS, COMMANDER_TRAIT_CONFIRM }
    public record Open(Mode mode,String target,List<String> selection) implements CustomPacketPayload {
        public Open {Objects.requireNonNull(mode);id(target,160);selection=List.copyOf(selection);if(selection.size()>192||selection.stream().distinct().count()!=selection.size())throw new IllegalArgumentException("Invalid commander selection");selection.forEach(s->id(s,160));}
        public Open(Mode mode,String target){this(mode,target,List.of());}
        public static final Type<Open> TYPE=new Type<>(Identifier.parse("hoi:commander_open_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Open> CODEC=StreamCodec.of((b,p)->{b.writeEnum(p.mode);b.writeUtf(p.target,160);b.writeVarInt(p.selection.size());p.selection.forEach(s->b.writeUtf(s,160));},b->{var mode=b.readEnum(Mode.class);var target=b.readUtf(160);int n=b.readVarInt();if(n<0||n>192)throw new IllegalArgumentException("Invalid commander selection count");var selection=new ArrayList<String>();for(int i=0;i<n;i++)selection.add(b.readUtf(160));return new Open(mode,target,selection);});
        @Override public Type<Open> type(){return TYPE;}
    }
    public record Ray(double x,double y,double z) {
        public Ray {double length=x*x+y*y+z*z;if(!Double.isFinite(length)||Math.abs(length-1)>.001||y>=-.00001)throw new IllegalArgumentException("Invalid land map ray");}
    }
    public record PlanMapPick(String token,long revision,boolean toggle,List<Ray> rays) implements CustomPacketPayload {
        public PlanMapPick {UUID.fromString(token);rays=List.copyOf(rays);if(revision<0||rays.isEmpty()||rays.size()>64||toggle&&rays.size()!=1)throw new IllegalArgumentException("Invalid land map selection");}
        public static final Type<PlanMapPick> TYPE=new Type<>(Identifier.parse("hoi:land_plan_map_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf,PlanMapPick> CODEC=StreamCodec.of((b,p)->{b.writeUtf(p.token,36);b.writeVarLong(p.revision);b.writeBoolean(p.toggle);b.writeVarInt(p.rays.size());for(var ray:p.rays){b.writeDouble(ray.x);b.writeDouble(ray.y);b.writeDouble(ray.z);}},b->{String token=b.readUtf(36);long revision=b.readVarLong();boolean toggle=b.readBoolean();int n=b.readVarInt();if(n<1||n>64)throw new IllegalArgumentException("Invalid land ray count");var rays=new ArrayList<Ray>();for(int i=0;i<n;i++)rays.add(new Ray(b.readDouble(),b.readDouble(),b.readDouble()));return new PlanMapPick(token,revision,toggle,rays);});
        @Override public Type<PlanMapPick> type(){return TYPE;}
    }
    public record PlanOverlay(String country,ProtocolAtlasScene.Page page) implements CustomPacketPayload {
        public PlanOverlay {if(country==null||!country.isEmpty()&&!country.matches("[A-Z0-9]{3}"))throw new IllegalArgumentException("Invalid plan country");Objects.requireNonNull(page);}
        public static final Type<PlanOverlay> TYPE=new Type<>(Identifier.parse("hoi:land_plan_overlay_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf,PlanOverlay> CODEC=StreamCodec.of((b,p)->{b.writeUtf(p.country,3);ProtocolAtlasScene.Page.CODEC.encode(b,p.page);},b->new PlanOverlay(b.readUtf(3),ProtocolAtlasScene.Page.CODEC.decode(b)));
        @Override public Type<PlanOverlay> type(){return TYPE;}
    }
    public record Card(String id,String name,String leader,String portrait,String marshal,String marshalPortrait,int count,int capacity,String detail,Mode role) {
        public Card(String id,String name,String leader,String portrait,String marshal,String marshalPortrait,int count,int capacity,String detail) {this(id,name,leader,portrait,marshal,marshalPortrait,count,capacity,detail,null);}
        public Card {ProtocolCommanderHud.id(id,160);text(name,160);text(leader,160);text(marshal,160);asset(portrait);asset(marshalPortrait);text(detail,2000);if(count<0||count>100000||capacity<0||capacity>100000)throw new IllegalArgumentException("Invalid command card");}
    }
    public record View(String country,String dimension,Mode branch,List<Card> cards) {
        public View {if(!country.isEmpty()&&!country.matches("[A-Z0-9_]{1,16}"))throw new IllegalArgumentException("Invalid command country");id(dimension,128);Objects.requireNonNull(branch);cards=List.copyOf(cards);if(cards.size()>160||cards.stream().map(Card::id).distinct().count()!=cards.size())throw new IllegalArgumentException("Invalid command cards");}
    }
    public record Snapshot(String json) implements CustomPacketPayload {
        public Snapshot {text(json,200000);}
        public static final Type<Snapshot> TYPE=new Type<>(Identifier.parse("hoi:commander_hud_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Snapshot> CODEC=StreamCodec.of((b,p)->b.writeUtf(p.json,200000),b->new Snapshot(b.readUtf(200000)));
        public static Snapshot of(View view){return new Snapshot(JSON.toJson(view));}
        public View view(){return ProtocolJsonCodec.read(JSON,json,View.class);}
        @Override public Type<Snapshot> type(){return TYPE;}
    }
    public static synchronized void registerPayloadTypes(){if(registered)return;PayloadTypeRegistry.serverboundPlay().register(Open.TYPE,Open.CODEC);PayloadTypeRegistry.clientboundPlay().register(Snapshot.TYPE,Snapshot.CODEC);PayloadTypeRegistry.serverboundPlay().register(PlanMapPick.TYPE,PlanMapPick.CODEC);PayloadTypeRegistry.clientboundPlay().register(PlanOverlay.TYPE,PlanOverlay.CODEC);registered=true;}
    private static void text(String s,int bound){if(s==null||s.length()>bound)throw new IllegalArgumentException("Invalid commander text");}
    private static void id(String s,int bound){text(s,bound);if(!s.isEmpty()&&!s.matches("[A-Za-z0-9_:/|.-]+"))throw new IllegalArgumentException("Invalid commander ID");}
    private static void asset(String s){text(s,180);if(!s.isEmpty()&&!s.matches("[a-z0-9_/-]+"))throw new IllegalArgumentException("Invalid commander portrait");}
}
