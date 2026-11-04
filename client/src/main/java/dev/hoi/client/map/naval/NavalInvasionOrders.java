package dev.hoi.client.map.naval;

import dev.hoi.protocol.commander.CommanderNavalInvasion;
import dev.hoi.client.map.agency.AgencyHud;
import dev.hoi.client.map.commander.CommanderOrders;
import dev.hoi.client.map.commander.CommanderPanels;
import java.util.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class NavalInvasionOrders {
    private static CommanderNavalInvasion.View view;
    private static Set<String> armies=Set.of();
    private static boolean stopping;
    private NavalInvasionOrders() {}
    public static void register(){ClientPlayNetworking.registerGlobalReceiver(CommanderNavalInvasion.Snapshot.TYPE,(packet,receiver)->{try{accept(packet.view());}catch(IllegalArgumentException ignored){}});}
    static void accept(CommanderNavalInvasion.View next){
        var context=CommanderOrders.currentContext();
        if(!CommanderOrders.valid()||!next.token().equals(context.token())||next.revision()!=context.revision()||!next.country().equals(context.country())||!next.dimension().equals(context.dimension()))return;
        view=next;
        if(view.plans().stream().noneMatch(p->armies.contains(p.army())))cancel();
    }
    static List<CommanderNavalInvasion.Plan> plans(){
        var context=CommanderOrders.currentContext();
        if(view==null||!CommanderOrders.valid()||!view.token().equals(context.token())||view.revision()!=context.revision()||!CommanderPanels.armyInput()||AgencyHud.active())return List.of();
        return view.plans();
    }
    public static boolean available(String army){return plans().stream().anyMatch(p->p.army().equals(army)&&!p.actions().isEmpty());}
    public static boolean begin(List<String> selected,boolean stop){
        if(!CommanderOrders.ready()||!CommanderOrders.pending().isEmpty())return false;
        if(plans().stream().noneMatch(p->selected.contains(p.army())))return false;
        armies=Set.copyOf(selected);stopping=stop;return true;
    }
    static boolean targeting(){return !armies.isEmpty();}
    public static boolean cancel(){boolean active=targeting();armies=Set.of();return active;}
    public static void reset(){cancel();view=null;NavalInvasionOverlay.clearHits();}
    static boolean execute(String id){
        if(!targeting()||!CommanderOrders.ready())return false;
        var plan=plans().stream().filter(p->p.id().equals(id)&&armies.contains(p.army())).findFirst().orElse(null);
        if(plan==null)return false;
        var action=stopping?(plan.phase().equals("RESERVED")?CommanderNavalInvasion.Action.UNRESERVE:CommanderNavalInvasion.Action.ABORT)
            :(plan.phase().equals("DRAFT")?CommanderNavalInvasion.Action.RESERVE:CommanderNavalInvasion.Action.LAUNCH);
        if(!plan.actions().contains(action)||action==CommanderNavalInvasion.Action.LAUNCH&&!plan.ready())return false;
        if(!CommanderOrders.send("INVASION/"+action.name(),List.of(plan.id(),plan.configuration()),List.of()))return false;
        cancel();return true;
    }
}
