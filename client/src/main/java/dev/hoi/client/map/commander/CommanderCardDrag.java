package dev.hoi.client.map.commander;

import dev.hoi.protocol.commander.CommanderHudPayloads.*;
import java.util.*;

final class CommanderCardDrag {
    record Slot(Card card,int x,int width) {}
    record Drop(String parent,List<String> order) {Drop {order=List.copyOf(order);}}
    private static View source;
    private static List<Slot> slots=List.of();
    private static String id="",gesture="";
    private static double start,position,destination;
    private static int creationX,rightLimit,leftLimit;
    private static boolean dragging;
    private static Drop queued;
    private static String queuedId="",queuedToken="";
    private static long queuedRevision;
    private static View queuedView;
    private static Set<String> queuedSelection=Set.of();
    private static int ticks;
    private CommanderCardDrag() {}
    static String parent(Card card){return card.panel()==null?"":card.panel().parent();}
    static boolean movable(Card card){return card.role()==Mode.ARMY_GROUP||card.role()==Mode.ASSIGN_ARMY||card.role()==Mode.NAVY_FLEET;}
    private static boolean movable(View view,Card card){return movable(card)||view.branch()==Mode.NAVY_HQ&&card.role()==null;}
    static List<Card> siblings(View view,Card card,String parent){return view.cards().stream().filter(c->view.branch()==Mode.NAVY_HQ?(c.role()==Mode.NAVY_FLEET||c.role()==null):c.role()==card.role()&&(card.role()!=Mode.ASSIGN_ARMY||parent(c).equals(parent))).toList();}
    static Drop drop(View view,List<Slot> slots,String id,double x){
        var card=view.cards().stream().filter(c->c.id().equals(id)&&movable(view,c)).findFirst().orElseThrow();
        var target=slots.stream().min(Comparator.comparingDouble(s->Math.abs(x-s.x()-s.width()/2.))).orElseThrow();
        String parent=parent(card);
        if(card.role()==Mode.ASSIGN_ARMY){
            parent=target.card().role()==Mode.ARMY_GROUP?target.card().id():target.card().role()==Mode.ASSIGN_ARMY?parent(target.card()):"";
            if(target.card().id().equals(id))parent=parent(card);
            if(x<=slots.getFirst().x()||x>=slots.getLast().x()+slots.getLast().width())parent="";
        }
        var siblings=siblings(view,card,parent);var order=new ArrayList<>(siblings.stream().map(Card::id).filter(k->!k.equals(id)).toList());
        int index=0;
        for(var slot:slots)if(order.contains(slot.card().id())&&x>slot.x()+slot.width()/2.)index=order.indexOf(slot.card().id())+1;
        if(card.role()==Mode.ARMY_GROUP){
            for(var slot:slots){String group=slot.card().role()==Mode.ARMY_GROUP?slot.card().id():parent(slot.card());if(order.contains(group)&&x>slot.x()+slot.width()/2.)index=order.indexOf(group)+1;}
        }
        order.add(index,id);
        if(parent.equals(parent(card))){
            var visible=new HashSet<String>();for(var slot:slots){visible.add(slot.card().id());if(card.role()==Mode.ARMY_GROUP)visible.add(parent(slot.card()));}
            var reordered=order.stream().filter(visible::contains).iterator();var complete=new ArrayList<String>();for(var sibling:siblings)complete.add(visible.contains(sibling.id())?reordered.next():sibling.id());order=complete;
        }
        return new Drop(parent,order);
    }
    static boolean begin(View view,List<CommanderHud.Box> boxes,double x,double y){
        if(!CommanderOrders.currentHud(view)||queued!=null||!CommanderOrders.pending().isEmpty())return false;
        var box=boxes.stream().filter(b->b.contains(x,y)).findFirst().orElse(null);
        if(box==null||box.request()==null||!Set.of(Mode.ASSIGN_ARMY,Mode.ASSIGN_GROUP_MARSHAL,Mode.NAVY_FLEET).contains(box.request().mode()))return false;
        var card=view.cards().stream().filter(c->c.id().equals(box.request().target())&&movable(view,c)).findFirst().orElse(null);if(card==null)return false;
        var visible=new ArrayList<Slot>();for(var b:boxes)if(b.request()!=null&&Set.of(Mode.ASSIGN_ARMY,Mode.ASSIGN_GROUP_MARSHAL,Mode.NAVY_FLEET).contains(b.request().mode()))view.cards().stream().filter(c->c.id().equals(b.request().target())&&movable(view,c)).findFirst().ifPresent(c->visible.add(new Slot(c,b.x(),b.width())));
        visible.sort(Comparator.comparingInt(Slot::x));
        creationX=boxes.stream().filter(b->b.request()!=null&&b.request().mode()==Mode.CREATE_ARMY).mapToInt(CommanderHud.Box::x).findFirst().orElse(Integer.MAX_VALUE);
        rightLimit=boxes.stream().filter(b->b.request()!=null&&b.request().mode()==Mode.CREATE_ARMY).mapToInt(b->b.x()+b.width()).findFirst().orElse(visible.getLast().x()+visible.getLast().width());
        leftLimit=Math.min(visible.getFirst().x(),boxes.stream().filter(b->b.request()!=null&&b.request().mode()==Mode.ARMY_GROUPS).mapToInt(CommanderHud.Box::x).findFirst().orElse(Integer.MAX_VALUE));
        source=view;slots=List.copyOf(visible);id=card.id();start=position=destination=x;dragging=false;gesture=CommanderOrders.gestureContext();return true;
    }
    static boolean move(double x){
        if(id.isEmpty())return false;if(!gesture.equals(CommanderOrders.gestureContext())){cancel();return true;}
        dragging|=Math.abs(x-start)>=3;
        var block=slots.stream().filter(s->moves(s.card())).toList();int left=block.getFirst().x(),right=block.getLast().x()+block.getLast().width();
        destination=Math.clamp(x,leftLimit,rightLimit);position=start+Math.clamp(x-start,leftLimit-left,rightLimit-right);return true;
    }
    private static Drop destinationDrop(){
        if(destination<=slots.getFirst().x())return edgeDrop(source,slots,id,true);
        if(destination<creationX)return drop(source,slots,id,destination);
        return edgeDrop(source,slots,id,false);
    }
    static Drop edgeDrop(View view,List<Slot> visible,String id,boolean front){
        var card=view.cards().stream().filter(c->c.id().equals(id)).findFirst().orElseThrow();
        var siblings=siblings(view,card,parent(card));var shown=new HashSet<String>();for(var slot:visible)shown.add(slot.card().id());
        var order=new ArrayList<>(siblings.stream().map(Card::id).filter(shown::contains).filter(key->!key.equals(id)).toList());order.add(front?0:order.size(),id);
        var reordered=order.iterator();var complete=new ArrayList<String>();for(var sibling:siblings)complete.add(shown.contains(sibling.id())?reordered.next():sibling.id());
        return new Drop(parent(card),complete);
    }
    private static boolean moves(Card card){return card.id().equals(id)||source!=null&&source.cards().stream().anyMatch(c->c.id().equals(id)&&c.role()==Mode.ARMY_GROUP)&&parent(card).equals(id);}
    static List<Card> preview(List<Card> visible){
        if(!dragging||id.isEmpty())return visible;var drop=destinationDrop();
        var block=visible.stream().filter(CommanderCardDrag::moves).toList();var result=new ArrayList<>(visible);result.removeAll(block);
        var moving=block.getFirst();int index=result.size();if(source.branch()==Mode.NAVY_HQ)for(int n=0;n<result.size();n++)if(result.get(n).role()==Mode.NAVY_HQ){index=n;break;}var order=drop.order();int own=order.indexOf(id);
        for(int i=own+1;i<order.size();i++){String next=order.get(i);int found=-1;for(int n=0;n<result.size();n++)if(result.get(n).id().equals(next)){found=n;break;}if(found>=0){index=found;break;}}
        if(moving.role()==Mode.ASSIGN_ARMY&&!drop.parent().isEmpty()&&own==order.size()-1){for(int n=0;n<result.size();n++)if(result.get(n).id().equals(drop.parent())||parent(result.get(n)).equals(drop.parent()))index=n+1;}
        if(moving.role()==Mode.ARMY_GROUP&&index==result.size()){index=0;for(int n=0;n<result.size();n++)if(result.get(n).role()==Mode.ARMY_GROUP||!CommanderHud.groups.owner(result.get(n),source).isEmpty())index=n+1;}
        result.addAll(index,block);return result;
    }
    static int x(Card card,int normal){
        if(!dragging||!moves(card))return normal;return slots.stream().filter(s->s.card().id().equals(card.id())).map(s->s.x()+(int)Math.round(position-start)).findFirst().orElse(normal);
    }
    static List<Integer> drawOrder(List<Card> cards){var indices=new ArrayList<Integer>();for(int i=0;i<cards.size();i++)indices.add(i);if(dragging)indices.sort(Comparator.comparing(i->moves(cards.get(i))));return indices;}
    static void accept(View next){if(source!=null&&(!source.country().equals(next.country())||!source.dimension().equals(next.dimension())||source.branch()!=next.branch()||!source.cards().stream().map(c->c.id()+"/"+parent(c)).toList().equals(next.cards().stream().map(c->c.id()+"/"+parent(c)).toList())))cancel();reconcile(next);}
    static boolean release(double x){
        if(id.isEmpty())return false;move(x);if(id.isEmpty())return true;
        String moved=id;var view=source;boolean dragged=dragging;Drop drop=dragged?destinationDrop():null;cancel();
        if(!dragged){dev.hoi.client.audio.AudioUiSounds.play(dev.hoi.client.audio.AudioUiSounds.selection(view.cards().stream().filter(c->c.id().equals(moved)).findFirst().orElseThrow().role()));CommanderPanels.select(moved);return true;}
        var card=view.cards().stream().filter(c->c.id().equals(moved)).findFirst().orElseThrow();
        if(card.role()==Mode.ASSIGN_ARMY&&!parent(card).equals(drop.parent())){
            if(view.branch()!=Mode.ARMY)return true;
            var values=new ArrayList<String>();if(!drop.parent().isEmpty())values.add("target/"+drop.parent());values.add(moved);
            if(CommanderOrders.sendFromHud(view,"FORMATION/"+(drop.parent().isEmpty()?"DETACH_ARMIES":"MOVE_ARMIES"),values)){dev.hoi.client.audio.AudioUiSounds.play("ui.click");queued=drop;queuedId=moved;queuedToken=view.orderToken();queuedRevision=view.orderRevision();queuedView=view;queuedSelection=Set.copyOf(CommanderPanels.selectedCards());ticks=100;}
        }else if(!siblings(view,card,drop.parent()).stream().map(Card::id).toList().equals(drop.order())&&CommanderOrders.sendFromHud(view,"CARD_ORDER",drop.order()))dev.hoi.client.audio.AudioUiSounds.play("ui.click");
        return true;
    }
    static void reconcile(View view){
        if(queued==null)return;var context=CommanderOrders.currentContext();
        if(context==null||!context.token().equals(queuedToken)||!queuedView.country().equals(view.country())||!queuedView.dimension().equals(view.dimension())||queuedView.branch()!=view.branch()||!queuedSelection.equals(CommanderPanels.selectedCards())){clearQueue();return;}
        if(!applied(view,queuedToken,queuedRevision)||!CommanderOrders.currentHud(view))return;
        var card=view.cards().stream().filter(c->c.id().equals(queuedId)).findFirst().orElse(null);
        if(card==null||!parent(card).equals(queued.parent())){clearQueue();return;}
        var actual=siblings(view,card,queued.parent()).stream().map(Card::id).toList();var order=queued.order();clearQueue();
        if(new HashSet<>(actual).equals(new HashSet<>(order))&&!actual.equals(order))CommanderOrders.sendFromHud(view,"CARD_ORDER",order);
    }
    static boolean applied(View view,String token,long revision){return view.orderToken()!=null&&token.equals(view.orderToken())&&view.orderRevision()>=revision;}
    static void tick(){if(queued!=null&&--ticks<=0)clearQueue();}
    static void cancel(){id="";source=null;slots=List.of();dragging=false;gesture="";}
    private static void clearQueue(){queued=null;queuedId="";queuedToken="";queuedView=null;queuedSelection=Set.of();ticks=0;}
    static void reset(){cancel();clearQueue();}
}
