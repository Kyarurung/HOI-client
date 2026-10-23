package dev.hoi.client.map;

import dev.hoi.client.ClientCampaignHud;
import dev.hoi.protocol.ProtocolCommanderHud.*;
import dev.hoi.protocol.ProtocolCommanderOrders;
import dev.hoi.protocol.ProtocolCommanderOrders.*;
import java.util.*;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class MapCommanderOrders {
    static Consumer<Command> commandObserver;
    static Consumer<Selection> selectionObserver;
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
    private static boolean heldOwned,heldClosing;
    private static ModeRequest afterHeld;
    private static String hint="";
    private static int hintTicks;
    record HintBounds(int x,int y,int width,int height) {}
    private static HintBounds hintBounds;
    static HintBounds hintBounds(){return hintBounds;}
    private MapCommanderOrders() {}
    public static void register(){
        ProtocolCommanderOrders.registerPayloadTypes();MapCommanderAvailability.register();
        ClientPlayNetworking.registerGlobalReceiver(Context.TYPE,(packet,receiver)->accept(packet));
        ClientPlayNetworking.registerGlobalReceiver(Details.TYPE,(packet,receiver)->{try{acceptDetails(packet.page());}catch(IllegalArgumentException ignored){}});
        ClientTickEvents.END_CLIENT_TICK.register(client->{MapCommanderCardDrag.tick();if(hintTicks>0)hintTicks--;if(context!=null&&!valid())reset();else selectionChanged();});
        ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->client.execute(()->{reset();retired.clear();}));
    }
    static Context currentContext(){return context;}
    static void accept(Context next){
        var client=Minecraft.getInstance();if(client.level==null||!next.country().equals(ClientCampaignHud.country())||!next.dimension().equals(client.level.dimension().identifier().toString())||retired.contains(next.token()))return;
        if(context!=null&&context.token().equals(next.token())&&next.revision()<context.revision())return;
        boolean fresh=context==null||!context.token().equals(next.token());
        if(fresh){if(context!=null)retired.add(context.token());sequence=0;detailGeneration=-1;detailPages.clear();published=null;waitingRevision=-1;cancelQueued=false;hint="";hintTicks=0;generation++;}
        context=next;if(fresh){heldAction="";heldOwned=false;heldClosing=false;afterHeld=null;heldMap=null;latched=null;resume=null;}if(waitingRevision>=0&&next.revision()>waitingRevision)waitingRevision=-1;
        long availabilityRequest=MapCommanderAvailability.request();selectionChanged();if(cancelQueued&&waitingRevision<0){cancelQueued=false;if(!next.pending().isEmpty())send("CANCEL",List.of(),List.of());}
        continueHeld();MapCommanderHud.reconcileCardOrder();MapCommanderAvailability.update(context,sequence-1,published,availabilityRequest==MapCommanderAvailability.request());
    }
    static void acceptDetails(DetailPage page){
        if(!valid()||!page.token().equals(context.token())||page.sequence()!=sequence-1||published==null||!published.equals(MapCommanderPanels.commandSelection())||page.generation()<detailGeneration)return;
        if(page.generation()>detailGeneration){detailGeneration=page.generation();detailTotal=page.total();detailPages.clear();}
        if(page.total()!=detailTotal)return;detailPages.put(page.offset(),page.members());
        if(detailPages.values().stream().mapToInt(List::size).sum()!=detailTotal)return;
        var members=detailPages.values().stream().flatMap(List::stream).toList();if(members.stream().map(Member::id).distinct().count()!=detailTotal)return;
        MapCommanderPanels.acceptDivisionDetails(members);selectionChanged();
    }
    static boolean valid(){var client=Minecraft.getInstance();return context!=null&&client.level!=null&&context.country().equals(ClientCampaignHud.country())&&context.dimension().equals(client.level.dimension().identifier().toString());}
    static void scopeChanged(View previous,View next){generation++;if(context!=null&&(!context.country().equals(next.country())||!context.dimension().equals(next.dimension())||previous.dimension().equals(next.dimension())&&previous.branch()!=next.branch()))reset();}
    static void reset(){MapCommanderCardDrag.reset();MapCommanderAvailability.reset();heldAction="";heldOwned=false;heldClosing=false;afterHeld=null;heldMap=null;latched=null;resume=null;if(context!=null)retired.add(context.token());context=null;sequence=0;detailGeneration=-1;detailPages.clear();published=null;waitingRevision=-1;cancelQueued=false;hint="";hintTicks=0;hintBounds=null;generation++;}
    public static boolean ready(){return valid()&&waitingRevision<0;}
    public static String pending(){if(!valid())return "";if(heldAction.isEmpty())return context.pending();return heldOwned&&(context.pending().startsWith(heldAction+"/")||heldAction.equals("LAND/EDIT_PLAN")&&context.pending().equals("LAND/AREA_STATES"))?context.pending():heldAction;}
    static void held(String type){
        String action=type.isEmpty()?"":"LAND/"+type;if(action.equals(heldAction))return;if(!action.isEmpty()&&!MapCommanderAvailability.reason(action).isEmpty())return;
        if(heldAction.isEmpty()&&!action.isEmpty())resume=latched!=null&&valid()&&context.pending().equals(latched.action())?latched:null;
        heldMap=null;boolean cancel=heldOwned;heldAction=action;heldOwned=false;generation++;if(cancel){heldClosing=true;cancelTarget();}if(action.isEmpty())continueHeld();
    }
    static void discardHeld(){resume=null;latched=null;afterHeld=null;held("");}
    static void availabilityChanged(){continueHeld();}
    private static void continueHeld(){
        if(!valid()||waitingRevision>=0||cancelQueued)return;
        if(heldMap!=null&&context.revision()>heldMap.revision()){
            var queued=heldMap;heldMap=null;if(heldAction.equals(queued.action())&&context.pending().equals(queued.action())&&MapCommanderPanels.commandSelection().equals(queued.selection()))send("MAP",queued.selection(),queued.rays());return;
        }
        if(heldClosing&&context.pending().isEmpty()){if(afterHeld!=null){var command=afterHeld;if(MapCommanderAvailability.waiting(command.action(),command.selection()))return;heldClosing=false;afterHeld=null;resume=null;send(command.action(),command.selection(),List.of());return;}heldClosing=false;}
        if(heldAction.isEmpty()&&resume!=null&&context.pending().isEmpty()){var saved=resume;if(MapCommanderAvailability.waiting(saved.action(),saved.selection()))return;resume=null;send(saved.action(),saved.selection(),List.of());}
    }
    public static boolean pointMode(){return Set.of("LAND/DELETE_PLAN","LAND/ASSIGN_UNITS","LAND/EDIT_PLAN","LAND/AREA_STATES","NAVAL_AREAS/REMOVE").contains(pending());}
    public static boolean clicksBorder(){return pending().equals("LAND/FRONT");}
    public static boolean editsPoint(){return pending().startsWith("LAND/EDIT_PLAN/");}
    public static boolean confirmDraft(){return MapCommanderPanels.armyCommandVisible()&&pending().equals("LAND/AREA_STATES")&&send("CONFIRM",List.of(),List.of());}
    public static boolean drawsPlanLine(){return pending().startsWith("LAND/")&&!pointMode();}
    public static String gestureContext(){return valid()?context.token()+"/"+context.revision()+"/"+generation+"/"+context.pending():"";}
    static void message(String text){hint=text;hintTicks=60;}
    static void drawMessage(net.minecraft.client.gui.GuiGraphicsExtractor graphics){
        hintBounds=null;String text=hintTicks>0?hint:"";if(text.isEmpty())return;
        var font=Minecraft.getInstance().font;var lines=dev.hoi.client.ui.UiTooltips.split(font,Component.literal(text),Math.max(1,graphics.guiWidth()-24));
        int width=lines.stream().mapToInt(font::width).max().orElse(0)+12,height=lines.size()*(font.lineHeight+2)+8,x=(graphics.guiWidth()-width)/2,y=graphics.guiHeight()-height-8;
        y=Math.min(y,MapCommanderHud.boxes().stream().mapToInt(MapCommanderHud.Box::y).min().orElse(graphics.guiHeight())-new dev.hoi.client.ui.UiUnits(graphics.guiWidth(),graphics.guiHeight()).size(32)-height-6);
        var occupied=new ArrayList<HintBounds>();for(var box:MapCommanderHud.boxes())occupied.add(new HintBounds(box.x(),box.y(),box.width(),box.height()));for(var hit:MapCommanderPanels.hits())occupied.add(new HintBounds(hit.x(),hit.y(),hit.w(),hit.h()));
        boolean shifted;do{shifted=false;for(var box:occupied)if(x<box.x()+box.width()&&x+width>box.x()&&y<box.y()+box.height()+4&&y+height+4>box.y()){y=box.y()-height-6;shifted=true;}}while(shifted);
        y=Math.max(dev.hoi.client.ui.UiMenuBar.height(graphics.guiWidth())+4,y);hintBounds=new HintBounds(x,y,width,height);graphics.fill(x,y,x+width,y+height,0xCC101418);
        int lineY=y+4;for(var line:lines){graphics.text(font,line,x+(width-font.width(line))/2,lineY,0xFFFFFFFF);lineY+=font.lineHeight+2;}
    }
    static void selectionScopeChanged(){resume=null;heldMap=null;generation++;if(valid()&&(!context.pending().isEmpty()||waitingRevision>=0))cancelTarget();}
    public static boolean cancelTarget(){
        heldMap=null;
        if(!valid()||context.pending().isEmpty()&&waitingRevision<0)return false;
        if(waitingRevision>=0)cancelQueued=true;else send("CANCEL",List.of(),List.of());return true;
    }
    public static void selectionChanged(){
        if(!valid())return;var keys=MapCommanderPanels.commandSelection();if(keys.equals(published)){MapCommanderAvailability.update(context,sequence-1,published,false);return;}
        if(keys.size()>2048){message("선택한 부대가 명령 입력 한도를 초과했습니다.");return;}
        if(selectionObserver==null&&!ClientPlayNetworking.canSend(Selection.TYPE))return;
        boolean changed=published!=null;var packet=new Selection(context.token(),sequence++,keys);published=keys;generation++;
        if(selectionObserver!=null)selectionObserver.accept(packet);else ClientPlayNetworking.send(packet);
        MapCommanderAvailability.update(context,sequence-1,published,false);
        if(changed){resume=null;heldMap=null;}
        if(changed&&(!context.pending().isEmpty()||waitingRevision>=0)){if(waitingRevision>=0)cancelQueued=true;else send("CANCEL",List.of(),List.of());}
    }
    static boolean send(String action,List<String> selection,List<Ray> rays){
        if(!valid()){message("현재 지도의 명령 정보를 기다리고 있습니다.");return false;}
        if(waitingRevision>=0)return false;
        if(selection.size()>2048||rays.size()>64){message("선택한 부대 또는 지도 경로가 명령 입력 한도를 초과했습니다.");return false;}
        if(commandObserver==null&&!ClientPlayNetworking.canSend(Command.TYPE)){message("직접 명령 연결을 사용할 수 없습니다.");return false;}
        selectionChanged();if(waitingRevision>=0||!MapCommanderAvailability.allow(action,selection))return false;var packet=new Command(context.token(),context.revision(),action,selection,rays);waitingRevision=context.revision();
        hint="";hintTicks=0;if(commandObserver!=null)commandObserver.accept(packet);else ClientPlayNetworking.send(packet);return true;
    }
    public static boolean map(List<Ray> rays){return map(rays,false);}
    public static boolean useMap(Minecraft client){
        if(client.player==null||client.gui.screen()!=null||!client.options.getCameraType().isFirstPerson()||!MapCommanderHud.cardsVisible()||!valid()||MapCommanderPanels.commandSelection().isEmpty())return false;
        if(!ready())return true;
        var matrix=client.gameRenderer.mainCamera().getViewRotationProjectionMatrix(new org.joml.Matrix4f()).invert();
        var vector=matrix.transformProject(new org.joml.Vector3f(0,0,1)).normalize();
        try{map(List.of(new Ray(vector.x,vector.y,vector.z)));}catch(IllegalArgumentException sky){}
        return true;
    }
    public static boolean map(List<Ray> rays,boolean pointEdit){
        if(rays.isEmpty())return false;var selected=MapCommanderPanels.commandSelection();if(selected.isEmpty()&&pending().isEmpty())return false;
        var points=drawsPlanLine()?rays:List.of(rays.getLast());
        if(clicksBorder()&&points.stream().allMatch(points.getFirst()::equals))points=List.of(points.getFirst());
        if(!heldAction.isEmpty()&&(!heldOwned||!context.pending().startsWith(heldAction)&&!context.pending().equals("LAND/AREA_STATES"))){var targets=heldAction.equals("LAND/ASSIGN_UNITS")?selected:MapCommanderPanels.armyIds();if(targets.isEmpty()){message("명령 대상을 선택하세요.");return false;}var queued=new HeldMap(heldAction,selected,points,context.revision());if(send(heldAction,targets,List.of())){heldOwned=true;heldMap=queued;return true;}return false;}
        return send(pointEdit&&editsPoint()?"MAP/POINT":"MAP",selected,pointEdit&&editsPoint()?List.of(rays.getFirst(),rays.getLast()):points);
    }
    static void land(String type,List<String> armies){if(pending().equals("LAND/"+type)){cancelTarget();return;}if(!MapCommanderAvailability.allow("LAND/"+type,armies))return;resume=null;latched=new ModeRequest("LAND/"+type,armies);if(!armies.isEmpty())send("LAND/"+type,armies,List.of());}
    static void assignSelected(){var ids=MapCommanderPanels.commandSelection();if(ids.isEmpty()){message("배정할 사단을 선택하세요.");return;}land("ASSIGN_UNITS",ids);}
    static void formation(String action,String target,List<String> selection){
        if(selection.isEmpty()){message(action.endsWith("ARMIES")||action.equals("CREATE_GROUP")?"편성할 군 카드를 선택하세요.":"편성할 사단을 선택하세요.");return;}
        var values=new ArrayList<String>();if(!target.isEmpty())values.add("target/"+target);values.addAll(selection);
        if(send("FORMATION/"+action,values,List.of())&&(action.equals("CREATE_ARMY")||action.equals("MOVE_DIVISIONS"))){MapCommanderPanels.mapSelectionChanged();if(action.equals("CREATE_ARMY"))MapCommanderPanels.awaitCreatedArmy(selection);}
    }
    static void repairPolicy(String force,Integer policy,boolean previous){
        if(policy==null)return;
        send("NAVAL_SETTING/"+(previous?"REPAIR_PREVIOUS":"REPAIR_NEXT"),List.of(force,"policy/"+policy),List.of());
    }
    static boolean route(Open request){
        if(request.mode()==Mode.AIR_ATTACH){
            if(request.selection().contains("action/detach")){
                var wings=request.target().isEmpty()?request.selection().stream().filter(id->!id.equals("action/detach")).toList():List.of(request.target());
                if(!wings.isEmpty())send("AIR_DETACH",wings.stream().map(id->"air/"+id).toList(),List.of());
            }else if(!request.selection().isEmpty()&&request.selection().getFirst().startsWith("army/")){
                var selected=MapCommanderPanels.commandSelection();if(selected.isEmpty()||selected.stream().anyMatch(id->!id.startsWith("air/")))return true;
                var values=new ArrayList<String>();values.add(request.selection().getFirst().substring(5));values.addAll(selected);send("AIR_ATTACH",values,List.of());
            }
            return true;
        }
        if(request.mode()==Mode.ARMY_ABILITIES){if(Set.of("force_attack","last_stand","staff_office_plan","extra_suplies").contains(request.target()))MapCommanderPanels.armyAbility(request.target());else message("사용할 스킬 버튼을 선택하세요.");return true;}
        if(Set.of(Mode.NAVAL_TASK_FORCES,Mode.NAVAL_MISSION,Mode.AIR_MISSION).contains(request.mode())&&MapCommanderPanels.openMission(request))return true;
        if(MapCommanderPanels.armyInput()&&(request.mode()==Mode.CREATE_ARMY||request.mode()==Mode.MOVE_DIVISIONS)){formation(request.mode().name(),request.target(),request.selection());return true;}
        if(MapCommanderPanels.armyInput()&&request.mode()==Mode.ARMY_GROUPS&&request.target().isEmpty()){formation("CREATE_GROUP","",MapCommanderPanels.formationArmies());return true;}
        if(request.mode()==Mode.ARMY_PLANS){MapCommanderPanels.openPlans(request.target());return true;}
        if(request.mode()==Mode.AIR_TOOLBAR||request.mode()==Mode.NAVAL_TOOLBAR){
            var values=request.selection();if(values.isEmpty()||!values.getFirst().startsWith("mission/"))return true;
            var ids=values.subList(1,values.size()).stream().filter(id->!id.startsWith("page/")).toList();
            if(!ids.isEmpty())send((request.mode()==Mode.AIR_TOOLBAR?"AIR/":"NAVY/")+values.getFirst().substring(8),ids,List.of());return true;
        }
        if(request.mode()==Mode.ARMY_TOOLBAR&&request.target().equals("REMOVE")&&!MapCommanderAvailability.allow("ARMY/REMOVE",request.selection()))return true;
        if(request.mode()==Mode.ARMY_TOOLBAR&&request.target().equals("AREA_STATES")){land("AREA_STATES",request.selection());return true;}
        if(request.mode()==Mode.ARMY_TOOLBAR&&request.target().startsWith("AREA_OPTION/")){send("ARMY/"+request.target(),request.selection(),List.of());return true;}
        if(request.mode()==Mode.ARMY_TOOLBAR&&Set.of("ACTIVATE","DEACTIVATE","CAREFUL","BALANCED","AGGRESSIVE","EXERCISE","EXERCISE_UNTIL_TRAINED","STOP_EXERCISE","HALT_UNITS","UNASSIGN_UNITS","STRATEGIC_UNITS","COHESION_ARMIES").contains(request.target())){
            if(!request.selection().isEmpty()){if(heldClosing&&waitingRevision>=0){afterHeld=new ModeRequest("ARMY/"+request.target(),request.selection());resume=null;}else send("ARMY/"+request.target(),request.selection(),List.of());}return true;
        }
        return false;
    }
}
