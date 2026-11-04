package dev.hoi.client.map.commander;

import dev.hoi.protocol.commander.CommanderOrderPayloads.*;
import java.util.*;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

final class CommanderAvailability {
    static Consumer<Check> queryObserver;
    private record Scope(String token,long revision,long sequence,List<String> selection,List<String> armies) {Scope {selection=List.copyOf(selection);armies=List.copyOf(armies);}}
    private static Scope scope;
    private static long request;
    private static Assessment assessment;
    private CommanderAvailability() {}
    static void register(){ClientPlayNetworking.registerGlobalReceiver(Availability.TYPE,(packet,context)->{try{accept(packet.assessment());}catch(IllegalArgumentException ignored){}});}
    static long request(){return request;}
    static void reset(){scope=null;assessment=null;}
    private static boolean supported(){return queryObserver!=null||ClientPlayNetworking.canSend(Check.TYPE);}
    static void update(Context context,long sequence,List<String> selection,boolean refresh) {
        if(!supported()||context==null||sequence<0||selection==null){reset();return;}
        var armies=CommanderPanels.armyInput()?CommanderPanels.armyIds().stream().sorted().toList():List.<String>of();
        var next=new Scope(context.token(),context.revision(),sequence,selection,armies);if(next.equals(scope)&&!refresh)return;
        if(!next.equals(scope))assessment=null;scope=next;var query=new Check(context.token(),context.revision(),sequence,++request,armies);
        if(queryObserver!=null)queryObserver.accept(query);else ClientPlayNetworking.send(query);
    }
    private static boolean current() {
        var context=CommanderOrders.currentContext();if(scope==null||context==null||!CommanderOrders.valid()||!scope.token().equals(context.token())||scope.revision()!=context.revision()||!scope.selection().equals(CommanderPanels.commandSelection()))return false;
        return scope.armies().equals(CommanderPanels.armyInput()?CommanderPanels.armyIds().stream().sorted().toList():List.of());
    }
    static void accept(Assessment next){if(!current()||!next.token().equals(scope.token())||next.revision()!=scope.revision()||next.sequence()!=scope.sequence()||next.request()!=request)return;assessment=next;CommanderOrders.availabilityChanged();}
    static String branchReason(String action){return !CommanderPanels.attachmentWings().isEmpty()&&(action.startsWith("ARMY/")||action.startsWith("LAND/")||action.startsWith("ABILITY/"))?"비행단을 선택한 상태입니다. 군이나 사단을 선택하세요.":"";}
    static String reason(String action) {
        String branch=branchReason(action);if(!branch.isEmpty())return branch;
        if(action.isEmpty()||!Set.of("AIR","NAVY","NAVAL_TB","LAND","ARMY","ABILITY").contains(action.split("/",2)[0])||!supported())return action.startsWith("NAVAL_TB/")?"사용 가능 여부를 확인하고 있습니다.":"";
        boolean mission=action.startsWith("AIR/")||action.startsWith("NAVY/");
        if(!current()||assessment==null)return mission?"":"사용 가능 여부를 확인하고 있습니다.";
        var status=assessment.actions().get(action);return status==null?(action.startsWith("NAVAL_TB/")?"사용 가능 여부를 확인하고 있습니다.":""):mission&&status.enabled()?"":!status.enabled()&&status.reason().isEmpty()?"현재 사용할 수 없습니다.":status.reason();
    }
    static String reason(String action,List<String> targets) {
        String branch=branchReason(action);if(!branch.isEmpty())return branch;
        List<String> expected;
        if(action.startsWith("AIR/")||action.startsWith("NAVY/"))expected=CommanderPanels.commandSelection().stream().map(k->k.substring(k.indexOf('/')+1)).toList();
        else if(action.startsWith("ABILITY/")||action.equals("LAND/ASSIGN_UNITS")||Set.of("ARMY/HALT_UNITS","ARMY/UNASSIGN_UNITS","ARMY/STRATEGIC_UNITS").contains(action))expected=CommanderPanels.commandSelection();
        else if(action.startsWith("ARMY/")||action.startsWith("LAND/"))expected=CommanderPanels.armyIds();
        else return "";
        return targets.size()==expected.size()&&new HashSet<>(targets).equals(new HashSet<>(expected))?reason(action):"";
    }
    static boolean waiting(String action,List<String> targets){return (!current()||assessment==null)&&!reason(action,targets).isEmpty();}
    static boolean allow(String action,List<String> targets){String reason=reason(action,targets);if(reason.isEmpty())return true;CommanderOrders.message(reason);return false;}
    static boolean warning(String action){var status=current()&&assessment!=null?assessment.actions().get(action):null;return status!=null&&Boolean.TRUE.equals(status.warning());}
    static String tooltip(String action,String original){var status=current()&&assessment!=null?assessment.actions().get(action):null;if(status!=null&&!status.details().isEmpty())original=action.startsWith("NAVY/")||action.startsWith("NAVAL_TB/")?status.details():original+"\n\n"+status.details();String reason=reason(action);return reason.isEmpty()||original.contains(reason)?original:explain(original,reason);}
    static String explain(String original,String reason){return reason.isEmpty()?original:original+"\n\n§R"+reason+"§!";}
    static boolean allow(String action){String reason=reason(action);if(reason.isEmpty())return true;CommanderOrders.message(reason);return false;}
}
