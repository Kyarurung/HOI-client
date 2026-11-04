package dev.hoi.client.map.commander;

import dev.hoi.protocol.commander.CommanderHudPayloads.*;
import java.util.*;

final class CommanderGroups {
    private final Map<String,Boolean> expanded=new HashMap<>();
    private final Set<String> foldedRows=new HashSet<>();
    private String scope="";
    void clear(){expanded.clear();foldedRows.clear();scope="";}
    void accept(View view){
        String next=view.country()+"/"+view.dimension()+"/"+(view.branch()==Mode.AIR?Mode.ARMY:view.branch());if(!next.equals(scope)){clear();scope=next;}
        var ids=view.cards().stream().map(Card::id).collect(java.util.stream.Collectors.toSet());expanded.keySet().retainAll(ids);foldedRows.retainAll(ids);view.cards().stream().filter(c->c.role()==Mode.ARMY_GROUP).forEach(c->expanded.putIfAbsent(c.id(),true));
    }
    boolean expanded(String id){return expanded.getOrDefault(id,false);}
    void reveal(Card card){if(card.role()==Mode.ARMY_GROUP)expanded.putIfAbsent(card.id(),true);else if(card.panel()!=null&&!card.panel().parent().isEmpty())expanded.putIfAbsent(card.panel().parent(),true);}
    void toggle(Card card){if(card.role()==Mode.ARMY_GROUP)expanded.put(card.id(),!expanded(card.id()));else if(card.role()==Mode.NAVY_FLEET&&!foldedRows.remove(card.id()))foldedRows.add(card.id());}
    boolean rowsVisible(String id){return !foldedRows.contains(id);}
    String owner(Card card,View view){
        if(card.role()==Mode.ARMY_GROUP)return expanded(card.id())?card.id():"";
        if(card.panel()==null||!expanded(card.panel().parent()))return "";
        return view.cards().stream().anyMatch(c->c.role()==Mode.ARMY_GROUP&&c.id().equals(card.panel().parent()))?card.panel().parent():"";
    }
    int color(String id,View view){return view.cards().stream().filter(c->c.id().equals(id)&&c.panel()!=null&&c.panel().color()!=null).map(c->0xff000000|c.panel().color()).findFirst().orElse(0xff777777);}
    List<List<Card>> pages(View view,int capacity){return pages(view,capacity,view.cards());}
    List<List<Card>> pages(View view,int capacity,List<Card> cards){
        capacity=Math.max(2,capacity);var blocks=new ArrayList<List<Card>>();var groups=cards.stream().filter(c->c.role()==Mode.ARMY_GROUP).toList();var grouped=new HashSet<String>();
        for(var group:groups){var children=cards.stream().filter(c->c.role()==Mode.ASSIGN_ARMY&&c.panel()!=null&&c.panel().parent().equals(group.id())).toList();children.forEach(c->grouped.add(c.id()));
            if(!expanded(group.id())||children.isEmpty()){blocks.add(List.of(group));continue;}
            for(int first=0;first<children.size();first+=capacity-1){var block=new ArrayList<Card>();block.add(group);block.addAll(children.subList(first,Math.min(children.size(),first+capacity-1)));blocks.add(List.copyOf(block));}
        }
        cards.stream().filter(c->c.role()!=Mode.ARMY_GROUP&&!grouped.contains(c.id())).forEach(c->blocks.add(List.of(c)));
        var result=new ArrayList<List<Card>>();var page=new ArrayList<Card>();
        for(var block:blocks){if(page.size()+block.size()>capacity){result.add(List.copyOf(page));page.clear();}page.addAll(block);}
        if(!page.isEmpty()||result.isEmpty())result.add(List.copyOf(page));return List.copyOf(result);
    }
}
