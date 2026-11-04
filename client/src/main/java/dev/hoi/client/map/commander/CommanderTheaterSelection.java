package dev.hoi.client.map.commander;

import dev.hoi.protocol.commander.CommanderHudPayloads.*;
import java.util.*;

final class CommanderTheaterSelection {
    private String country="",army="",navy="";
    void clear(){country=army=navy="";}
    private static List<Theater> theaters(View view){return view.branch()==Mode.AIR?view.armyTheaters():view.theaters();}
    String selected(View view){
        if(!country.equals(view.country())){clear();country=view.country();}
        var theaters=theaters(view);String id=view.branch()==Mode.NAVY_HQ?navy:army;
        if(!theaters.isEmpty()&&theaters.stream().noneMatch(t->t.id().equals(id))){choose(view,theaters.getFirst().id());return theaters.getFirst().id();}
        return id;
    }
    boolean choose(View view,String id){
        if(theaters(view).stream().noneMatch(t->t.id().equals(id)))return false;
        if(view.branch()==Mode.NAVY_HQ){boolean changed=!navy.equals(id);navy=id;return changed;}
        boolean changed=!army.equals(id);army=id;return changed;
    }
    boolean selectCard(View view,String id){
        var theater=theaters(view).stream().filter(t->contains(t,id)).findFirst().orElse(null);
        return theater!=null&&choose(view,theater.id());
    }
    List<Card> cards(View view){
        String selected=selected(view);var theater=theaters(view).stream().filter(t->t.id().equals(selected)).findFirst().orElse(null);
        return theater==null?view.cards():view.cards().stream().filter(c->contains(theater,c.id())).toList();
    }
    private static boolean contains(Theater theater,String id){return theater.groups().contains(id)||theater.armies().contains(id)||theater.fleets().contains(id)||theater.reserves().contains(id);}
}
