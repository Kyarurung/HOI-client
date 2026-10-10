package dev.hoi.client.map;

import dev.hoi.protocol.ProtocolCommanderOrders.*;
import java.util.*;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

final class MapCommanderAvailability {
    static Consumer<Check> queryObserver;
    private record Scope(String token,long revision,long sequence,List<String> selection,List<String> armies) {Scope {selection=List.copyOf(selection);armies=List.copyOf(armies);}}
    private static Scope scope;
    private static long request;
    private static Assessment assessment;
    private MapCommanderAvailability() {}
    static void register(){ClientPlayNetworking.registerGlobalReceiver(Availability.TYPE,(packet,context)->{try{accept(packet.assessment());}catch(IllegalArgumentException ignored){}});}
    static long request(){return request;}
    static void reset(){scope=null;assessment=null;}
    private static boolean supported(){return queryObserver!=null||ClientPlayNetworking.canSend(Check.TYPE);}
    static void update(Context context,long sequence,List<String> selection,boolean refresh) {
        if(!supported()||context==null||sequence<0||selection==null){reset();return;}
        var armies=MapCommanderPanels.armyInput()?MapCommanderPanels.armyIds().stream().sorted().toList():List.<String>of();
        var next=new Scope(context.token(),context.revision(),sequence,selection,armies);if(next.equals(scope)&&!refresh)return;
        if(!next.equals(scope))assessment=null;scope=next;var query=new Check(context.token(),context.revision(),sequence,++request,armies);
        if(queryObserver!=null)queryObserver.accept(query);else ClientPlayNetworking.send(query);
    }
    private static boolean current() {
        var context=MapCommanderOrders.currentContext();if(scope==null||context==null||!MapCommanderOrders.valid()||!scope.token().equals(context.token())||scope.revision()!=context.revision()||!scope.selection().equals(MapCommanderPanels.commandSelection()))return false;
        return scope.armies().equals(MapCommanderPanels.armyInput()?MapCommanderPanels.armyIds().stream().sorted().toList():List.of());
    }
    static void accept(Assessment next){if(!current()||!next.token().equals(scope.token())||next.revision()!=scope.revision()||next.sequence()!=scope.sequence()||next.request()!=request)return;assessment=next;MapCommanderOrders.availabilityChanged();}
    static String branchReason(String action){return !MapCommanderPanels.attachmentWings().isEmpty()&&(action.startsWith("ARMY/")||action.startsWith("LAND/")||action.startsWith("ABILITY/"))?"비행단을 선택한 상태입니다. 군이나 사단을 선택하세요.":"";}
    static String reason(String action) {
        String branch=branchReason(action);if(!branch.isEmpty())return branch;
        if(action.isEmpty()||!Set.of("AIR","NAVY","LAND","ARMY","ABILITY").contains(action.split("/",2)[0])||!supported())return "";
        if(!current()||assessment==null)return "사용 가능 여부를 확인하고 있습니다.";
        var status=assessment.actions().get(action);return status==null?"":status.reason();
    }
    static String reason(String action,List<String> targets) {
        String branch=branchReason(action);if(!branch.isEmpty())return branch;
        List<String> expected;
        if(action.startsWith("AIR/")||action.startsWith("NAVY/"))expected=MapCommanderPanels.commandSelection().stream().map(k->k.substring(k.indexOf('/')+1)).toList();
        else if(action.startsWith("ABILITY/")||action.equals("LAND/ASSIGN_UNITS")||Set.of("ARMY/HALT_UNITS","ARMY/UNASSIGN_UNITS","ARMY/STRATEGIC_UNITS").contains(action))expected=MapCommanderPanels.commandSelection();
        else if(action.startsWith("ARMY/")||action.startsWith("LAND/"))expected=MapCommanderPanels.armyIds();
        else return "";
        return targets.size()==expected.size()&&new HashSet<>(targets).equals(new HashSet<>(expected))?reason(action):"";
    }
    static boolean waiting(String action,List<String> targets){return (!current()||assessment==null)&&!reason(action,targets).isEmpty();}
    static boolean allow(String action,List<String> targets){String reason=reason(action,targets);if(reason.isEmpty())return true;MapCommanderOrders.message(reason);return false;}
    static String tooltip(String action,String original){String reason=reason(action);if(!reason.isEmpty())return explain(original,reason);var status=current()&&assessment!=null?assessment.actions().get(action):null;if(status!=null&&!status.details().isEmpty())original+="\n\n"+status.details();return original;}
    static String explain(String original,String reason){return reason.isEmpty()?original:original+"\n\n§R"+reason+"§!";}
    static boolean allow(String action){String reason=reason(action);if(reason.isEmpty())return true;MapCommanderOrders.message(reason);return false;}
}
