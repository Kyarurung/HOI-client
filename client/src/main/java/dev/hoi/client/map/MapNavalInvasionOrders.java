package dev.hoi.client.map;

import dev.hoi.protocol.ProtocolNavalInvasion;
import java.util.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

final class MapNavalInvasionOrders {
    private static ProtocolNavalInvasion.View view;
    private static Set<String> armies=Set.of();
    private static boolean stopping;
    private MapNavalInvasionOrders() {}
    static void register(){ClientPlayNetworking.registerGlobalReceiver(ProtocolNavalInvasion.Snapshot.TYPE,(packet,receiver)->{try{accept(packet.view());}catch(IllegalArgumentException ignored){}});}
    static void accept(ProtocolNavalInvasion.View next){
        var context=MapCommanderOrders.currentContext();
        if(!MapCommanderOrders.valid()||!next.token().equals(context.token())||next.revision()!=context.revision()||!next.country().equals(context.country())||!next.dimension().equals(context.dimension()))return;
        view=next;
        if(view.plans().stream().noneMatch(p->armies.contains(p.army())))cancel();
    }
    static List<ProtocolNavalInvasion.Plan> plans(){
        var context=MapCommanderOrders.currentContext();
        if(view==null||!MapCommanderOrders.valid()||!view.token().equals(context.token())||view.revision()!=context.revision()||!MapCommanderPanels.armyInput()||MapAgencyHud.active())return List.of();
        return view.plans();
    }
    static boolean available(String army){return plans().stream().anyMatch(p->p.army().equals(army)&&!p.actions().isEmpty());}
    static boolean begin(List<String> selected,boolean stop){
        if(!MapCommanderOrders.ready()||!MapCommanderOrders.pending().isEmpty())return false;
        if(plans().stream().noneMatch(p->selected.contains(p.army())))return false;
        armies=Set.copyOf(selected);stopping=stop;return true;
    }
    static boolean targeting(){return !armies.isEmpty();}
    static boolean cancel(){boolean active=targeting();armies=Set.of();return active;}
    static void reset(){cancel();view=null;MapNavalInvasionOverlay.clearHits();}
    static boolean execute(String id){
        if(!targeting()||!MapCommanderOrders.ready())return false;
        var plan=plans().stream().filter(p->p.id().equals(id)&&armies.contains(p.army())).findFirst().orElse(null);
        if(plan==null)return false;
        var action=stopping?(plan.phase().equals("RESERVED")?ProtocolNavalInvasion.Action.UNRESERVE:ProtocolNavalInvasion.Action.ABORT)
            :(plan.phase().equals("DRAFT")?ProtocolNavalInvasion.Action.RESERVE:ProtocolNavalInvasion.Action.LAUNCH);
        if(!plan.actions().contains(action)||action==ProtocolNavalInvasion.Action.LAUNCH&&!plan.ready())return false;
        if(!MapCommanderOrders.send("INVASION/"+action.name(),List.of(plan.id(),plan.configuration()),List.of()))return false;
        cancel();return true;
    }
}
