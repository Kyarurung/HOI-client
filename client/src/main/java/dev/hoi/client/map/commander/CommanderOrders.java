package dev.hoi.client.map.commander;

import dev.hoi.client.ClientCampaignHud;
import dev.hoi.protocol.commander.CommanderHudPayloads.*;
import dev.hoi.protocol.commander.CommanderOrderPayloads;
import dev.hoi.protocol.commander.CommanderOrderPayloads.*;
import dev.hoi.client.map.naval.NavalInvasionOrders;
import java.util.*;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

public final class CommanderOrders {
    public static Consumer<Command> commandObserver;
    public static Consumer<Selection> selectionObserver;
    private static Context context;
    private static long sequence,generation,waitingRevision=-1;
    private static List<String> published;
    private static final Set<String> retired=new HashSet<>();
    private static boolean cancelQueued;
    private static long detailGeneration=-1;
    private static int detailTotal;
    private static final Map<Integer,List<Member>> detailPages=new TreeMap<>();
    private record ModeRequest(String action,List<String> selection) {ModeRequest {selection=List.copyOf(selection);}}
    private record HeldMap(String action,List<String> selection,List<Ray> rays,long revision) {HeldMap {selection=List.copyOf(selection);rays=List.copyOf(rays);}}
    private static String heldAction="";
    private static ModeRequest latched,resume;
    private static HeldMap heldMap;
    private static HeldMap queuedMove;
    private static boolean heldOwned,heldClosing;
    private static ModeRequest afterHeld;
    private CommanderOrders() {}
    public static void register(){
        CommanderOrderPayloads.registerPayloadTypes();CommanderAvailability.register();NavalInvasionOrders.register();
        ClientPlayNetworking.registerGlobalReceiver(Context.TYPE,(packet,receiver)->accept(packet));
        ClientPlayNetworking.registerGlobalReceiver(Details.TYPE,(packet,receiver)->{try{acceptDetails(packet.page());}catch(IllegalArgumentException ignored){}});
        ClientTickEvents.END_CLIENT_TICK.register(client->{CommanderCardDrag.tick();if(context!=null&&!valid())reset();else selectionChanged();});
        ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->client.execute(()->{reset();retired.clear();}));
    }
    public static Context currentContext(){return context;}
    public static void accept(Context next){
        var client=Minecraft.getInstance();if(client.level==null||!next.country().equals(ClientCampaignHud.country())||!next.dimension().equals(client.level.dimension().identifier().toString())||retired.contains(next.token()))return;
        if(context!=null&&context.token().equals(next.token())&&next.revision()<context.revision())return;
        boolean fresh=context==null||!context.token().equals(next.token());
        if(fresh){if(context!=null)retired.add(context.token());sequence=0;detailGeneration=-1;detailPages.clear();published=null;waitingRevision=-1;cancelQueued=false;generation++;}
        context=next;if(fresh){heldAction="";heldOwned=false;heldClosing=false;afterHeld=null;heldMap=null;queuedMove=null;latched=null;resume=null;}if(waitingRevision>=0&&next.revision()>waitingRevision)waitingRevision=-1;
        long availabilityRequest=CommanderAvailability.request();selectionChanged();if(cancelQueued&&waitingRevision<0){cancelQueued=false;if(!next.pending().isEmpty())send("CANCEL",List.of(),List.of());}
        continueHeld();continueMove();CommanderHud.reconcileCardOrder();CommanderAvailability.update(context,sequence-1,published,availabilityRequest==CommanderAvailability.request());
    }
    static void acceptDetails(DetailPage page){
        if(!valid()||!page.token().equals(context.token())||page.sequence()!=sequence-1||published==null||!published.equals(CommanderPanels.commandSelection())||page.generation()<detailGeneration)return;
        if(page.generation()>detailGeneration){detailGeneration=page.generation();detailTotal=page.total();detailPages.clear();}
        if(page.total()!=detailTotal)return;detailPages.put(page.offset(),page.members());
        if(detailPages.values().stream().mapToInt(List::size).sum()!=detailTotal)return;
        var members=detailPages.values().stream().flatMap(List::stream).toList();if(members.stream().map(Member::id).distinct().count()!=detailTotal)return;
        CommanderPanels.acceptDivisionDetails(members);selectionChanged();
    }
    public static boolean valid(){var client=Minecraft.getInstance();return context!=null&&client.level!=null&&context.country().equals(ClientCampaignHud.country())&&context.dimension().equals(client.level.dimension().identifier().toString());}
    static void scopeChanged(View previous,View next){generation++;if(context!=null&&(!context.country().equals(next.country())||!context.dimension().equals(next.dimension())||previous.dimension().equals(next.dimension())&&previous.branch()!=next.branch()))reset();}
    static void reset(){NavalInvasionOrders.reset();CommanderCardDrag.reset();CommanderAvailability.reset();heldAction="";heldOwned=false;heldClosing=false;afterHeld=null;heldMap=null;queuedMove=null;latched=null;resume=null;if(context!=null)retired.add(context.token());context=null;sequence=0;detailGeneration=-1;detailPages.clear();published=null;waitingRevision=-1;cancelQueued=false;generation++;}
    public static boolean ready(){return valid()&&waitingRevision<0;}
    public static String pending(){if(!valid())return "";if(heldAction.isEmpty())return context.pending();return heldOwned&&(context.pending().startsWith(heldAction+"/")||heldAction.equals("LAND/EDIT_PLAN")&&context.pending().equals("LAND/AREA_STATES"))?context.pending():heldAction;}
    static void held(String type){
        String action=type.isEmpty()?"":"LAND/"+type;if(action.equals(heldAction))return;if(!action.isEmpty()&&!CommanderAvailability.reason(action).isEmpty())return;
        if(heldAction.isEmpty()&&!action.isEmpty())resume=latched!=null&&valid()&&context.pending().equals(latched.action())?latched:null;
        heldMap=null;boolean cancel=heldOwned;heldAction=action;heldOwned=false;generation++;if(cancel){heldClosing=true;cancelTarget();}if(action.isEmpty())continueHeld();
    }
    static void discardHeld(){resume=null;latched=null;afterHeld=null;held("");}
    static void availabilityChanged(){continueHeld();}
    private static void continueHeld(){
        if(!valid()||waitingRevision>=0||cancelQueued)return;
        if(heldMap!=null&&context.revision()>heldMap.revision()){
            var queued=heldMap;heldMap=null;if(heldAction.equals(queued.action())&&context.pending().equals(queued.action())&&CommanderPanels.commandSelection().equals(queued.selection()))send("MAP",queued.selection(),queued.rays());return;
        }
        if(heldClosing&&context.pending().isEmpty()){if(afterHeld!=null){var command=afterHeld;if(CommanderAvailability.waiting(command.action(),command.selection()))return;heldClosing=false;afterHeld=null;resume=null;send(command.action(),command.selection(),List.of());return;}heldClosing=false;}
        if(heldAction.isEmpty()&&resume!=null&&context.pending().isEmpty()){var saved=resume;if(CommanderAvailability.waiting(saved.action(),saved.selection()))return;resume=null;send(saved.action(),saved.selection(),List.of());}
    }
    public static boolean pointMode(){return Set.of("LAND/DELETE_PLAN","LAND/ASSIGN_UNITS","LAND/EDIT_PLAN","LAND/AREA_STATES","NAVAL_AREAS/REMOVE").contains(pending());}
    public static boolean clicksBorder(){return pending().equals("LAND/FRONT");}
    public static boolean editsPoint(){return pending().startsWith("LAND/EDIT_PLAN/");}
    public static boolean confirmDraft(){return CommanderPanels.armyCommandVisible()&&pending().equals("LAND/AREA_STATES")&&send("CONFIRM",List.of(),List.of());}
    public static boolean drawsPlanLine(){return pending().startsWith("LAND/")&&!pointMode();}
    public static boolean targetTool(){return pending().startsWith("LAND/")||pending().startsWith("NAVAL_AREAS/");}
    public static String gestureContext(){return valid()?context.token()+"/"+(targetTool()?context.revision():"")+"/"+generation+"/"+context.pending():"";}
    private static void continueMove(){
        if(queuedMove==null||!ready()||cancelQueued)return;
        var queued=queuedMove;queuedMove=null;if(!targetTool()&&queued.selection().equals(CommanderPanels.commandSelection()))send("MAP",queued.selection(),queued.rays());
    }
    static void selectionScopeChanged(){resume=null;heldMap=null;generation++;if(valid()&&(!context.pending().isEmpty()||waitingRevision>=0))cancelTarget();}
    public static boolean cancelTarget(){
        if(NavalInvasionOrders.cancel())return true;
        heldMap=null;queuedMove=null;
        if(!valid()||context.pending().isEmpty()&&waitingRevision<0)return false;
        if(waitingRevision>=0)cancelQueued=true;else send("CANCEL",List.of(),List.of());return true;
    }
    public static void selectionChanged(){
        if(!valid())return;var keys=CommanderPanels.commandSelection();if(keys.equals(published)){CommanderAvailability.update(context,sequence-1,published,false);return;}
        if(keys.size()>2048)return;
        if(selectionObserver==null&&!ClientPlayNetworking.canSend(Selection.TYPE))return;
        boolean changed=published!=null;var packet=new Selection(context.token(),sequence++,keys);published=keys;generation++;
        if(selectionObserver!=null)selectionObserver.accept(packet);else ClientPlayNetworking.send(packet);
        CommanderAvailability.update(context,sequence-1,published,false);
        if(changed){NavalInvasionOrders.cancel();resume=null;heldMap=null;queuedMove=null;}
        if(changed&&(!context.pending().isEmpty()||waitingRevision>=0)){if(waitingRevision>=0)cancelQueued=true;else send("CANCEL",List.of(),List.of());}
    }
    static boolean currentHud(dev.hoi.protocol.commander.CommanderHudPayloads.View view){
        return ready()&&view.orderToken()!=null&&view.orderToken().equals(context.token())&&view.orderRevision()==context.revision();
    }
    static boolean sendFromHud(dev.hoi.protocol.commander.CommanderHudPayloads.View view,String action,List<String> selection){
        return currentHud(view)&&send(action,selection,List.of());
    }
    public static boolean send(String action,List<String> selection,List<Ray> rays){
        if(!valid())return false;
        if(waitingRevision>=0)return false;
        if(selection.size()>2048||rays.size()>64)return false;
        if(commandObserver==null&&!ClientPlayNetworking.canSend(Command.TYPE))return false;
        selectionChanged();if(waitingRevision>=0||!CommanderAvailability.allow(action,selection))return false;var packet=new Command(context.token(),context.revision(),action,selection,rays);waitingRevision=context.revision();
        if(commandObserver!=null)commandObserver.accept(packet);else ClientPlayNetworking.send(packet);return true;
    }
    public static boolean map(List<Ray> rays){return map(rays,false);}
    public static boolean useMap(Minecraft client){
        if(client.player==null||client.gui.screen()!=null||!client.options.getCameraType().isFirstPerson()||!CommanderHud.cardsVisible()||!valid()||CommanderPanels.commandSelection().isEmpty())return false;
        var matrix=client.gameRenderer.mainCamera().getViewRotationProjectionMatrix(new org.joml.Matrix4f()).invert();
        var vector=matrix.transformProject(new org.joml.Vector3f(0,0,1)).normalize();
        try{map(List.of(new Ray(vector.x,vector.y,vector.z)));}catch(IllegalArgumentException sky){}
        return true;
    }
    public static boolean map(List<Ray> rays,boolean pointEdit){
        if(!valid())return false;
        if(rays.isEmpty())return false;var selected=CommanderPanels.commandSelection();if(selected.isEmpty()&&pending().isEmpty())return false;
        var points=drawsPlanLine()?rays:List.of(targetTool()?rays.getLast():rays.getFirst());
        if(!targetTool()){
            if(selected.size()>2048)return false;
            selectionChanged();if(waitingRevision>=0){queuedMove=new HeldMap("MAP",selected,points,context.revision());return true;}
        }
        if(clicksBorder()&&points.stream().allMatch(points.getFirst()::equals))points=List.of(points.getFirst());
        if(!heldAction.isEmpty()&&(!heldOwned||!context.pending().startsWith(heldAction)&&!context.pending().equals("LAND/AREA_STATES"))){var targets=heldAction.equals("LAND/ASSIGN_UNITS")?selected:CommanderPanels.armyIds();if(targets.isEmpty())return false;var queued=new HeldMap(heldAction,selected,points,context.revision());if(send(heldAction,targets,List.of())){heldOwned=true;heldMap=queued;return true;}return false;}
        return send(pointEdit&&editsPoint()?"MAP/POINT":"MAP",selected,pointEdit&&editsPoint()?List.of(rays.getFirst(),rays.getLast()):points);
    }
    static void land(String type,List<String> armies){if(pending().equals("LAND/"+type)){cancelTarget();return;}if(!CommanderAvailability.allow("LAND/"+type,armies))return;resume=null;latched=new ModeRequest("LAND/"+type,armies);if(!armies.isEmpty())send("LAND/"+type,armies,List.of());}
    static void assignSelected(){var ids=CommanderPanels.commandSelection();if(ids.isEmpty())return;land("ASSIGN_UNITS",ids);}
    static void formation(String action,String target,List<String> selection){
        if(selection.isEmpty())return;
        var values=new ArrayList<String>();if(!target.isEmpty())values.add("target/"+target);values.addAll(selection);
        if(send("FORMATION/"+action,values,List.of())&&(action.equals("CREATE_ARMY")||action.equals("MOVE_DIVISIONS"))){CommanderPanels.mapSelectionChanged();if(action.equals("CREATE_ARMY"))CommanderPanels.awaitCreatedArmy(selection);}
    }
    static void repairPolicy(String force,Integer policy,boolean previous){
        if(policy==null)return;
        send("NAVAL_SETTING/"+(previous?"REPAIR_PREVIOUS":"REPAIR_NEXT"),List.of(force,"policy/"+policy),List.of());
    }
    static boolean route(Open request){
        if(request.mode()==Mode.ARMY_TOOLBAR&&Set.of("ACTIVATE","DEACTIVATE").contains(request.target())&&CommanderPanels.shift()&&NavalInvasionOrders.begin(request.selection(),request.target().equals("DEACTIVATE")))return true;
        if(request.mode()==Mode.AIR_ATTACH){
            if(request.selection().contains("action/detach")){
                var wings=request.target().isEmpty()?request.selection().stream().filter(id->!id.equals("action/detach")).toList():List.of(request.target());
                if(!wings.isEmpty())send("AIR_DETACH",wings.stream().map(id->"air/"+id).toList(),List.of());
            }else if(!request.selection().isEmpty()&&request.selection().getFirst().startsWith("army/")){
                var selected=CommanderPanels.commandSelection();if(selected.isEmpty()||selected.stream().anyMatch(id->!id.startsWith("air/")))return true;
                var values=new ArrayList<String>();values.add(request.selection().getFirst().substring(5));values.addAll(selected);send("AIR_ATTACH",values,List.of());
            }
            return true;
        }
        if(request.mode()==Mode.ARMY_ABILITIES){if(Set.of("force_attack","last_stand","staff_office_plan","extra_suplies").contains(request.target()))CommanderPanels.armyAbility(request.target());return true;}
        if(Set.of(Mode.NAVAL_TASK_FORCES,Mode.NAVAL_MISSION,Mode.AIR_MISSION).contains(request.mode())&&CommanderPanels.openMission(request))return true;
        if(CommanderPanels.armyInput()&&(request.mode()==Mode.CREATE_ARMY||request.mode()==Mode.MOVE_DIVISIONS)){formation(request.mode().name(),request.target(),request.selection());return true;}
        if(CommanderPanels.armyInput()&&request.mode()==Mode.ARMY_GROUPS&&request.target().isEmpty()){formation("CREATE_GROUP","",CommanderPanels.formationArmies());return true;}
        if(request.mode()==Mode.ARMY_PLANS){CommanderPanels.openPlans(request.target());return true;}
        if(request.mode()==Mode.AIR_TOOLBAR||request.mode()==Mode.NAVAL_TOOLBAR){
            var values=request.selection();if(values.isEmpty()||!values.getFirst().startsWith("mission/"))return true;
            var ids=values.subList(1,values.size()).stream().filter(id->!id.startsWith("page/")).toList();
            if(!ids.isEmpty())send((request.mode()==Mode.AIR_TOOLBAR?"AIR/":"NAVY/")+values.getFirst().substring(8),ids,List.of());return true;
        }
        if(request.mode()==Mode.ARMY_TOOLBAR&&request.target().equals("REMOVE")&&!CommanderAvailability.allow("ARMY/REMOVE",request.selection()))return true;
        if(request.mode()==Mode.ARMY_TOOLBAR&&request.target().equals("AREA_STATES")){land("AREA_STATES",request.selection());return true;}
        if(request.mode()==Mode.ARMY_TOOLBAR&&request.target().startsWith("AREA_OPTION/")){send("ARMY/"+request.target(),request.selection(),List.of());return true;}
        if(request.mode()==Mode.ARMY_TOOLBAR&&Set.of("ACTIVATE","DEACTIVATE","CAREFUL","BALANCED","AGGRESSIVE","EXERCISE","EXERCISE_UNTIL_TRAINED","STOP_EXERCISE","HALT_UNITS","UNASSIGN_UNITS","STRATEGIC_UNITS","COHESION_ARMIES").contains(request.target())){
            if(!request.selection().isEmpty()){if(heldClosing&&waitingRevision>=0){afterHeld=new ModeRequest("ARMY/"+request.target(),request.selection());resume=null;}else send("ARMY/"+request.target(),request.selection(),List.of());}return true;
        }
        return false;
    }
}
