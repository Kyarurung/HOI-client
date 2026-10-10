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
    public enum Mode { ARMY, FIELD_OFFICERS, NAVY, NAVY_HQ, AIR, ASSIGN_ARMY, ASSIGN_MARSHAL, PROMOTE, CREATE_ARMY, MOVE_DIVISIONS, NAVAL_TASK_FORCES, NAVAL_MISSION, AIR_MISSION, ARMY_PLANS, LAND_PLAN, ARMY_UNITS, PLAN_UNITS, ARMY_GROUPS, ARMY_GROUP, ARMY_GROUP_MEMBERS, ASSIGN_GROUP_MARSHAL, NAVY_FLEET, NAVY_FLEET_MEMBERS, NAVAL_ORDERS, AIR_ORDERS, NAVY_HEADQUARTERS, COMMANDER_TRAITS, COMMANDER_TRAIT_CONFIRM, COMMANDER_TACTICS, COMMANDER_TACTIC_CONFIRM, COMMANDER_ADVISOR_ROLES, COMMANDER_ADVISOR_CONFIRM, AIR_ATTACH, NAVAL_TOOLBAR, AIR_TOOLBAR, ARMY_TOOLBAR, AIR_ORGANIZE, NAVAL_SETTINGS, NAVAL_INVASION_PLANS, NAVAL_ORGANIZE, AIR_GROUPS, AIR_REINFORCEMENT, NAVAL_AREAS, NAVAL_REINFORCEMENT, ARMY_ABILITIES }
    public record Open(Mode mode,String target,List<String> selection) implements CustomPacketPayload {
        public Open {Objects.requireNonNull(mode);id(target,160);selection=List.copyOf(selection);if(selection.size()>2048||selection.stream().distinct().count()!=selection.size())throw new IllegalArgumentException("Invalid commander selection");selection.forEach(s->id(s,160));}
        public Open(Mode mode,String target){this(mode,target,List.of());}
        public static final Type<Open> TYPE=new Type<>(Identifier.parse("hoi:commander_open_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Open> CODEC=StreamCodec.of((b,p)->{b.writeEnum(p.mode);b.writeUtf(p.target,160);b.writeVarInt(p.selection.size());p.selection.forEach(s->b.writeUtf(s,160));},b->{var mode=b.readEnum(Mode.class);var target=b.readUtf(160);int n=b.readVarInt();if(n<0||n>2048)throw new IllegalArgumentException("Invalid commander selection count");var selection=new ArrayList<String>();for(int i=0;i<n;i++)selection.add(b.readUtf(160));return new Open(mode,target,selection);});
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
    public record MovementQuad(float x,float y,float z,float width,float length,float yaw,float u0,float v0,float u1,float v1) {
        public MovementQuad {
            for(float coordinate:new float[]{x,y,z,yaw})if(!Float.isFinite(coordinate)||Math.abs(coordinate)>65536)throw new IllegalArgumentException("Invalid movement coordinate");
            for(float size:new float[]{width,length})if(!Float.isFinite(size)||size<=0||size>8192)throw new IllegalArgumentException("Invalid movement size");
            for(float uv:new float[]{u0,v0,u1,v1})if(!Float.isFinite(uv)||uv<0||uv>1)throw new IllegalArgumentException("Invalid movement UV");
            if(u0>=u1||v0>=v1)throw new IllegalArgumentException("Empty movement UV");
        }
        private void write(RegistryFriendlyByteBuf b){for(float value:new float[]{x,y,z,width,length,yaw,u0,v0,u1,v1})b.writeFloat(value);}
        private static MovementQuad read(RegistryFriendlyByteBuf b){return new MovementQuad(b.readFloat(),b.readFloat(),b.readFloat(),b.readFloat(),b.readFloat(),b.readFloat(),b.readFloat(),b.readFloat(),b.readFloat(),b.readFloat());}
    }
    public record PlanOverlay(String country,ProtocolAtlasScene.Page page,List<MovementQuad> movement) implements CustomPacketPayload {
        public PlanOverlay(String country,ProtocolAtlasScene.Page page){this(country,page,List.of());}
        public PlanOverlay {if(country==null||!country.isEmpty()&&!country.matches("[A-Z0-9]{3}"))throw new IllegalArgumentException("Invalid plan country");Objects.requireNonNull(page);movement=List.copyOf(movement);if(movement.size()>ProtocolAtlasScene.PAGE_SIZE)throw new IllegalArgumentException("Oversized movement page");}
        public static final Type<PlanOverlay> TYPE=new Type<>(Identifier.parse("hoi:land_plan_overlay_v2"));
        public static final StreamCodec<RegistryFriendlyByteBuf,PlanOverlay> CODEC=StreamCodec.of((b,p)->{b.writeUtf(p.country,3);ProtocolAtlasScene.Page.CODEC.encode(b,p.page);b.writeVarInt(p.movement.size());p.movement.forEach(q->q.write(b));},b->{String country=b.readUtf(3);var page=ProtocolAtlasScene.Page.CODEC.decode(b);int count=b.readVarInt();if(count<0||count>ProtocolAtlasScene.PAGE_SIZE)throw new IllegalArgumentException("Invalid movement page size");var quads=new ArrayList<MovementQuad>(count);for(int i=0;i<count;i++)quads.add(MovementQuad.read(b));return new PlanOverlay(country,page,quads);});
        @Override public Type<PlanOverlay> type(){return TYPE;}
    }
    public record Card(String id,String name,String leader,String portrait,String marshal,String marshalPortrait,int count,int capacity,String detail,Mode role,Panel panel) {
        public Card(String id,String name,String leader,String portrait,String marshal,String marshalPortrait,int count,int capacity,String detail,Mode role) {this(id,name,leader,portrait,marshal,marshalPortrait,count,capacity,detail,role,null);}
        public Card(String id,String name,String leader,String portrait,String marshal,String marshalPortrait,int count,int capacity,String detail) {this(id,name,leader,portrait,marshal,marshalPortrait,count,capacity,detail,null);}
        public Card {ProtocolCommanderHud.id(id,160);text(name,160);text(leader,160);text(marshal,160);asset(portrait);asset(marshalPortrait);text(detail,2000);if(count<0||count>100000||capacity<0||capacity>100000)throw new IllegalArgumentException("Invalid command card");}
    }
    public record Member(String id,String name,String status,String icon,String detail,double strength,Open request,String selectionKey,Double organization,Map<String,Integer> composition,String group,String mission,MemberData data) {
        public Member(String id,String name,String status,String icon,String detail,double strength,Open request,String selectionKey,Double organization,Map<String,Integer> composition,String group,String mission){this(id,name,status,icon,detail,strength,request,selectionKey,organization,composition,group,mission,null);}
        public Member(String id,String name,String status,String icon,String detail,double strength,Open request,String selectionKey,Double organization,Map<String,Integer> composition,String group){this(id,name,status,icon,detail,strength,request,selectionKey,organization,composition,group,"");}
        public Member(String id,String name,String status,String icon,String detail,double strength,Open request,String selectionKey){this(id,name,status,icon,detail,strength,request,selectionKey,null,Map.of(),"");}
        public Member(String id,String name,String status,String icon,String detail,double strength,Open request){this(id,name,status,icon,detail,strength,request,"");}
        public Member {mission=mission==null?"":mission;ProtocolCommanderHud.id(mission,64);group=group==null?"":group;text(group,160);composition=composition==null?Map.of():Map.copyOf(composition);if(composition.size()>16||composition.values().stream().anyMatch(n->n<0||n>100000)||organization!=null&&(!Double.isFinite(organization)||organization<0||organization>1))throw new IllegalArgumentException("Invalid command composition");composition.keySet().forEach(k->asset(k));selectionKey=selectionKey==null?"":selectionKey;ProtocolCommanderHud.id(selectionKey,160);ProtocolCommanderHud.id(id,160);text(name,160);text(status,180);asset(icon);text(detail,1200);if(!Double.isFinite(strength)||strength< -1||strength>1)throw new IllegalArgumentException("Invalid command strength");Objects.requireNonNull(request);}
    }
    public record MemberData(String template,String attachedArmy,int quantity,Integer insignia,NavalData navy,Boolean reinforce,AirData air,Boolean strategic) {
        public MemberData(String template,String attachedArmy,int quantity,Integer insignia,NavalData navy,Boolean reinforce,AirData air){this(template,attachedArmy,quantity,insignia,navy,reinforce,air,null);}
        public MemberData(String template,String attachedArmy,int quantity,Integer insignia,NavalData navy,Boolean reinforce){this(template,attachedArmy,quantity,insignia,navy,reinforce,null);}
        public MemberData(String template,String attachedArmy,int quantity,Integer insignia){this(template,attachedArmy,quantity,insignia,null,null);}
        public MemberData {template=template==null?"":template;attachedArmy=attachedArmy==null?"":attachedArmy;id(template,160);id(attachedArmy,160);if(quantity<0||quantity>1000000||insignia!=null&&(insignia<0||insignia>=81))throw new IllegalArgumentException("Invalid command member identity");}
    }
    public record AirBase(String id,int ownPlanes,int totalPlanes,double capacity,String controller) {
        public AirBase(String id,int ownPlanes,int totalPlanes,double capacity){this(id,ownPlanes,totalPlanes,capacity,"");}
        public AirBase {controller=controller==null?"":controller;ProtocolCommanderHud.id(controller,32);ProtocolCommanderHud.id(id,160);if(id.isEmpty()||ownPlanes<0||totalPlanes<ownPlanes||totalPlanes>1000000000||!Double.isFinite(capacity)||capacity<0||capacity>1000000000)throw new IllegalArgumentException("Invalid air base state");}
    }
    public record AirWingDetails(int target,String sortieTime,String equipmentIcon,boolean carrier,String missionName) {
        public AirWingDetails {if(target<1||target>1000000||sortieTime==null||!Set.of("DAY","NIGHT","BOTH").contains(sortieTime))throw new IllegalArgumentException("Invalid air wing details");asset(equipmentIcon);text(missionName,200);}
    }
    public record AirData(String airGroup,String airGroupName,String reinforcementPolicy,AirBase base,AirWingDetails wing) {
        public AirData(String airGroup,String airGroupName,String reinforcementPolicy,AirBase base){this(airGroup,airGroupName,reinforcementPolicy,base,null);}
        public AirData(String airGroup,String airGroupName,String reinforcementPolicy){this(airGroup,airGroupName,reinforcementPolicy,null);}
        public AirData {airGroup=airGroup==null?"":airGroup;airGroupName=airGroupName==null?"":airGroupName;id(airGroup,160);text(airGroupName,160);if(!Set.of("ANY","OBSOLETE_ONLY","ONLY_NEW","ONLY_SAME").contains(reinforcementPolicy)||airGroup.isEmpty()!=airGroupName.isEmpty())throw new IllegalArgumentException("Invalid air control state");}
    }
    public record NavalData(Integer repairPolicy,Boolean autoSplit,Boolean reinforce,String engagement,Map<String,Integer> targets,Boolean convoyExcluded,Set<String> excludedTargets) {
        public NavalData(Integer repairPolicy,Boolean autoSplit,Boolean reinforce,String engagement,Map<String,Integer> targets,Boolean convoyExcluded){this(repairPolicy,autoSplit,reinforce,engagement,targets,convoyExcluded,null);}
        public Set<String> resolvedExcludedTargets(){return excludedTargets!=null?excludedTargets:Boolean.TRUE.equals(convoyExcluded)?Set.of("CONVOY"):Set.of();}
        public NavalData(Integer repairPolicy,Boolean autoSplit,Boolean reinforce,String engagement,Map<String,Integer> targets){this(repairPolicy,autoSplit,reinforce,engagement,targets,null);}
        public NavalData {if(excludedTargets!=null){excludedTargets=Set.copyOf(excludedTargets);if(!Set.of("SUBMARINE","DESTROYER","LIGHT_CRUISER","HEAVY_CRUISER","CONVOY","BATTLE_CRUISER","BATTLESHIP","SH_BATTLESHIP","CARRIER").containsAll(excludedTargets))throw new IllegalArgumentException("Invalid naval targets");}if(repairPolicy!=null&&(repairPolicy<0||repairPolicy>3)||!Set.of("NEVER","LOW","MEDIUM","HIGH","ALWAYS").contains(engagement))throw new IllegalArgumentException("Invalid naval control state");targets=targets==null?Map.of():Map.copyOf(targets);if(targets.size()>16||targets.values().stream().anyMatch(n->n<0||n>1000))throw new IllegalArgumentException("Invalid naval composition target");targets.keySet().forEach(ProtocolCommanderHud::asset);}
    }
    public record Control(String id,String icon,String tooltip,boolean enabled,Open request) {
        public Control {ProtocolCommanderHud.id(id,80);asset(icon);text(tooltip,6000);Objects.requireNonNull(request);}
    }
    public record Panel(String parent,String commander,ProtocolDialogView.Leader leader,List<Member> members,int total,List<Control> controls,List<String> divisionIds,Integer color,List<Integer> palette) {
        public Panel(String parent,String commander,ProtocolDialogView.Leader leader,List<Member> members,int total,List<Control> controls,List<String> divisionIds){this(parent,commander,leader,members,total,controls,divisionIds,null,List.of());}
        public Panel(String parent,String commander,ProtocolDialogView.Leader leader,List<Member> members,int total,List<Control> controls){this(parent,commander,leader,members,total,controls,null);}
        public Panel(String parent,String commander,ProtocolDialogView.Leader leader,List<Member> members,int total){this(parent,commander,leader,members,total,List.of());}
        public Panel {palette=palette==null?List.of():List.copyOf(palette);if(color!=null&&(color<0||color>0xffffff)||palette.size()>64||palette.stream().anyMatch(c->c==null||c<0||c>0xffffff))throw new IllegalArgumentException("Invalid command color");controls=controls==null?List.of():List.copyOf(controls);if(controls.size()>20)throw new IllegalArgumentException("Invalid command controls");id(parent,160);id(commander,160);members=List.copyOf(members);if(members.size()>120||total<members.size()||total>100000||members.stream().map(Member::id).distinct().count()!=members.size())throw new IllegalArgumentException("Invalid command members");if(divisionIds==null&&members.size()==total&&members.stream().allMatch(m->m.request().mode()==Mode.ARMY_UNITS))divisionIds=members.stream().map(Member::id).toList();if(divisionIds!=null){divisionIds=List.copyOf(divisionIds);if(divisionIds.size()>2048||divisionIds.size()!=total||new HashSet<>(divisionIds).size()!=divisionIds.size())throw new IllegalArgumentException("Invalid army membership");divisionIds.forEach(s->{id(s,160);if(s.isEmpty())throw new IllegalArgumentException("Empty army member");});}}
    }
    public record View(String country,String dimension,Mode branch,List<Card> cards,List<Member> wings) {
        public View(String country,String dimension,Mode branch,List<Card> cards){this(country,dimension,branch,cards,List.of());}
        public View {if(!country.isEmpty()&&!country.matches("[A-Z0-9_]{1,16}"))throw new IllegalArgumentException("Invalid command country");id(dimension,128);Objects.requireNonNull(branch);cards=List.copyOf(cards);wings=wings==null?List.of():List.copyOf(wings);if(wings.size()>160||wings.stream().map(Member::id).distinct().count()!=wings.size()||cards.stream().filter(c->c.panel()!=null).mapToInt(c->c.panel().members().size()).sum()+wings.size()>512)throw new IllegalArgumentException("Invalid command detail budget");if(cards.size()>160||cards.stream().map(Card::id).distinct().count()!=cards.size())throw new IllegalArgumentException("Invalid command cards");}
    }
    public record Snapshot(String json) implements CustomPacketPayload {
        public Snapshot {text(json,1000000);if(json.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>1000000)throw new IllegalArgumentException("Commander packet exceeds byte budget");}
        public static final Type<Snapshot> TYPE=new Type<>(Identifier.parse("hoi:commander_hud_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Snapshot> CODEC=StreamCodec.of((b,p)->b.writeUtf(p.json,1000000),b->new Snapshot(b.readUtf(1000000)));
        public static Snapshot of(View view){
            String json=JSON.toJson(view);if(bytes(json)<=900000)return new Snapshot(json);
            var cards=view.cards().stream().map(c->new Card(c.id(),c.name(),c.leader(),c.portrait(),c.marshal(),c.marshalPortrait(),c.count(),c.capacity(),c.detail(),c.role(),c.panel()==null?null:new Panel(c.panel().parent(),c.panel().commander(),null,List.of(),c.panel().total(),List.of(),c.panel().divisionIds()))).toList();
            json=JSON.toJson(new View(view.country(),view.dimension(),view.branch(),cards,view.wings()));if(bytes(json)<=900000)return new Snapshot(json);
            cards=cards.stream().map(c->new Card(c.id(),c.name(),c.leader(),c.portrait(),c.marshal(),c.marshalPortrait(),c.count(),c.capacity(),"",c.role(),c.panel())).toList();
            json=JSON.toJson(new View(view.country(),view.dimension(),view.branch(),cards,view.wings()));if(bytes(json)<=900000)return new Snapshot(json);
            var wings=view.wings().stream().map(w->new Member(w.id(),w.name(),w.status(),w.icon(),"",w.strength(),w.request(),w.selectionKey(),w.organization(),w.composition(),w.group(),w.mission(),w.data())).toList();
            json=JSON.toJson(new View(view.country(),view.dimension(),view.branch(),cards,wings));if(bytes(json)<=1000000)return new Snapshot(json);
            cards=cards.stream().map(c->new Card(c.id(),c.name(),c.leader(),c.portrait(),c.marshal(),c.marshalPortrait(),c.count(),c.capacity(),c.detail(),c.role(),c.panel()==null?null:new Panel(c.panel().parent(),c.panel().commander(),null,List.of(),c.panel().total(),List.of(),null))).toList();
            return new Snapshot(JSON.toJson(new View(view.country(),view.dimension(),view.branch(),cards,wings)));
        }
        private static int bytes(String value){return value.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;}
        public View view(){return ProtocolJsonCodec.read(JSON,json,View.class);}
        @Override public Type<Snapshot> type(){return TYPE;}
    }
    public static synchronized void registerPayloadTypes(){if(registered)return;PayloadTypeRegistry.serverboundPlay().registerLarge(Open.TYPE,Open.CODEC,400000);PayloadTypeRegistry.clientboundPlay().register(Snapshot.TYPE,Snapshot.CODEC);PayloadTypeRegistry.serverboundPlay().register(PlanMapPick.TYPE,PlanMapPick.CODEC);PayloadTypeRegistry.clientboundPlay().register(PlanOverlay.TYPE,PlanOverlay.CODEC);registered=true;}
    private static void text(String s,int bound){if(s==null||s.length()>bound)throw new IllegalArgumentException("Invalid commander text");}
    private static void id(String s,int bound){text(s,bound);if(!s.isEmpty()&&!s.matches("[A-Za-z0-9_:/|.-]+"))throw new IllegalArgumentException("Invalid commander ID");}
    private static void asset(String s){text(s,180);if(!s.isEmpty()&&!s.matches("[a-z0-9_/-]+"))throw new IllegalArgumentException("Invalid commander portrait");}
}
