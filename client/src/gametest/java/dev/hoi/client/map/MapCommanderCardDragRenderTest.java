package dev.hoi.client.map;

import com.mojang.blaze3d.platform.InputConstants;
import dev.hoi.client.screen.ScreenMapCursor;
import dev.hoi.protocol.ProtocolCommanderHud.*;
import dev.hoi.protocol.ProtocolCommanderOrders;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;

final class MapCommanderCardDragRenderTest {
    private static MouseButtonEvent left(double x,double y){return new MouseButtonEvent(x,y,new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT,0));}
    private static MapCommanderHud.Box portrait(String id){return MapCommanderHud.boxes().stream().filter(b->b.request()!=null&&b.request().target().equals(id)&&Set.of(Mode.ASSIGN_ARMY,Mode.ASSIGN_GROUP_MARSHAL).contains(b.request().mode())).findFirst().orElseThrow();}
    private static void ack(){var c=MapCommanderOrders.currentContext();MapCommanderOrders.accept(new ProtocolCommanderOrders.Context(c.token(),c.revision()+1,c.country(),c.dimension(),""));}
    private static Card copy(Card card,String parent,int count){var p=card.panel();return new Card(card.id(),card.name(),card.leader(),card.portrait(),card.marshal(),card.marshalPortrait(),count,card.capacity(),card.detail(),card.role(),new Panel(parent,p.commander(),p.leader(),p.members(),p.total(),p.controls(),p.divisionIds(),p.color(),p.palette(),p.experience(),p.divisionTypes(),p.manpower(),p.planPower(),p.commandCaption(),p.organization(),p.strength(),p.executionMode(),p.planEvaluation(),p.abilities()));}
    private static View ordered(View view,List<String> ids){var cards=new ArrayList<>(view.cards());var ordered=ids.stream().map(id->cards.stream().filter(c->c.id().equals(id)).findFirst().orElseThrow()).toList();int n=0;for(int i=0;i<cards.size();i++)if(ids.contains(cards.get(i).id()))cards.set(i,ordered.get(n++));return new View(view.country(),view.dimension(),view.branch(),cards,view.wings(),view.running());}
    private static View attached(View view,String id,String parent){var cards=view.cards().stream().map(c->c.id().equals(id)?copy(c,parent,c.count()):c).toList();cards=cards.stream().map(c->c.role()==Mode.ARMY_GROUP?copy(c,"",(int)view.cards().stream().filter(a->a.role()==Mode.ASSIGN_ARMY&&(a.id().equals(id)?parent:MapCommanderCardDrag.parent(a)).equals(c.id())).count()):c).toList();return new View(view.country(),view.dimension(),view.branch(),cards,view.wings(),view.running());}
    static void run(ClientGameTestContext game,int[] size,View source){
        var commands=new ArrayList<ProtocolCommanderOrders.Command>();var view=new View[]{source};
        game.runOnClient(client->{MapCommanderHud.reset();MapCommanderHud.accept(view[0]);MapCommanderOrders.commandObserver=commands::add;MapCommanderOrders.selectionObserver=s->{};MapCommanderOrders.accept(new ProtocolCommanderOrders.Context(UUID.randomUUID().toString(),0,source.country(),source.dimension(),""));MapCommanderPanels.select("G1",false);MapCommanderPanels.select("G2",true);});
        game.setScreen(ScreenMapCursor::new);game.waitTicks(2);
        game.takeScreenshot("hoi-army-drag-before-"+size[0]+"x"+size[1]+"-gui2");
        game.runOnClient(client->{var a=portrait("A");var screen=client.gui.screen();var down=left(a.x()+a.width()/2.,a.y()+a.height()/2.);screen.mouseClicked(down,false);if(!MapCommanderPanels.selectedCards().equals(Set.of("G1","G2")))throw new AssertionError("Card press must wait for click release");screen.mouseDragged(left(down.x(),down.y()-80),0,-80);screen.mouseReleased(left(down.x(),down.y()-80));if(!MapCommanderPanels.selectedCards().equals(Set.of("A"))||!commands.isEmpty())throw new AssertionError("Vertical-only movement must remain a click without a card-order command");});game.waitTicks(2);
        var drag=new double[3];var origin=new int[2];
        game.runOnClient(client->{var a=portrait("A");origin[0]=a.x();origin[1]=a.width();drag[0]=a.x()+a.width()/2.;drag[1]=a.y()+a.height()/2.;drag[2]=drag[0]+1000;client.gui.screen().mouseClicked(left(drag[0],drag[1]),false);client.gui.screen().mouseDragged(left(drag[2],drag[1]-100),1000,-100);});game.waitTicks(2);
        game.runOnClient(client->{if(portrait("A").y()!=portrait("B").y()||portrait("A").x()!=origin[0]+origin[1])throw new AssertionError("Far right drag must clamp to one card width and keep row height");});game.takeScreenshot("hoi-army-drag-horizontal-preview-"+size[0]+"x"+size[1]+"-gui2");
        game.runOnClient(client->{client.gui.screen().mouseReleased(left(drag[2],drag[1]-100));assertCommand(commands,1,"CARD_ORDER",List.of("B","A"));if(!view[0].cards().stream().filter(c->c.panel()!=null&&c.panel().parent().equals("G1")).map(Card::id).toList().equals(List.of("A","B")))throw new AssertionError("Local drag cannot publish saved order");ack();view[0]=ordered(view[0],List.of("B","A"));MapCommanderHud.accept(view[0]);});game.waitTicks(2);game.takeScreenshot("hoi-army-drag-sibling-after-"+size[0]+"x"+size[1]+"-gui2");
        game.runOnClient(client->{var group=portrait("G1");origin[0]=group.x();origin[1]=group.width();drag[0]=group.x()+group.width()/2.;drag[1]=group.y()+group.height()/2.;drag[2]=drag[0]+1000;client.gui.screen().mouseClicked(left(drag[0],drag[1]),false);client.gui.screen().mouseDragged(left(drag[2],drag[1]),1000,0);});game.waitTicks(2);
        game.runOnClient(client->{var group=portrait("G1");var a=portrait("A");var b=portrait("B");if(group.x()!=origin[0]+origin[1]||!(group.x()<b.x()&&b.x()<a.x()))throw new AssertionError("Moving a group must clamp to one card width and carry the complete ordered child block");});game.takeScreenshot("hoi-army-drag-group-preview-"+size[0]+"x"+size[1]+"-gui2");
        game.runOnClient(client->{client.gui.screen().mouseReleased(left(drag[2],drag[1]));if(commands.size()!=1)throw new AssertionError("One-slot group drag cannot cross its expanded children");for(var card:view[0].cards())if(card.role()==Mode.ARMY_GROUP&&MapCommanderHud.groups.expanded(card.id()))MapCommanderHud.groups.toggle(card);});game.waitTicks(2);
        game.runOnClient(client->{var group=portrait("G1");var screen=client.gui.screen();var down=left(group.x()+group.width()/2.,group.y()+group.height()/2.);screen.mouseClicked(down,false);screen.mouseDragged(left(down.x()+1000,down.y()),1000,0);screen.mouseReleased(left(down.x()+1000,down.y()));assertCommand(commands,2,"CARD_ORDER",List.of("G2","G1"));ack();view[0]=ordered(view[0],List.of("G2","G1"));MapCommanderHud.accept(view[0]);for(var card:view[0].cards())if(card.role()==Mode.ARMY_GROUP&&!MapCommanderHud.groups.expanded(card.id()))MapCommanderHud.groups.toggle(card);});game.waitTicks(2);game.takeScreenshot("hoi-army-drag-group-after-"+size[0]+"x"+size[1]+"-gui2");
        game.runOnClient(client->{var a=portrait("A");var screen=client.gui.screen();drag[0]=a.x()+a.width()/2.;drag[1]=a.y()+a.height()/2.;drag[2]=a.x()+a.width()+200;screen.mouseClicked(left(drag[0],drag[1]),false);screen.mouseDragged(left(drag[2],drag[1]+90),200,90);screen.mouseReleased(left(drag[2],drag[1]+90));if(commands.size()!=2)throw new AssertionError("Plus drop must keep the existing parent and never create an army");view[0]=attached(view[0],"A","");MapCommanderHud.accept(view[0]);});game.waitTicks(2);game.takeScreenshot("hoi-army-drag-detached-synthetic-input-"+size[0]+"x"+size[1]+"-gui2");
        game.runOnClient(client->{view[0]=ordered(view[0],List.of("G1","G2"));MapCommanderHud.accept(view[0]);});game.waitTicks(2);
        game.runOnClient(client->{var a=portrait("A");origin[0]=a.x();origin[1]=a.width();drag[0]=a.x()+a.width()/2.;drag[1]=a.y()+a.height()/2.;drag[2]=drag[0]-1000;var screen=client.gui.screen();screen.mouseClicked(left(drag[0],drag[1]),false);screen.mouseDragged(left(drag[2],drag[1]),-1000,0);});game.waitTicks(2);
        game.runOnClient(client->{if(portrait("A").x()!=origin[0]-origin[1])throw new AssertionError("Repeated far left drag must clamp from its new start to one card width");});game.takeScreenshot("hoi-army-drag-left-limit-"+size[0]+"x"+size[1]+"-gui2");
        game.runOnClient(client->{client.gui.screen().mouseReleased(left(drag[2],drag[1]));assertCommand(commands,3,"FORMATION/MOVE_ARMIES",List.of("target/G2","A"));view[0]=ordered(attached(view[0],"A","G2"),List.of("C","A"));MapCommanderHud.accept(view[0]);if(commands.size()!=3)throw new AssertionError("HUD alone cannot release the revision lock");ack();assertCommand(commands,4,"CARD_ORDER",List.of("A","C"));ack();view[0]=ordered(view[0],List.of("A","C"));MapCommanderHud.accept(view[0]);});game.waitTicks(2);game.takeScreenshot("hoi-army-drag-attached-after-"+size[0]+"x"+size[1]+"-gui2");
        game.runOnClient(client->{var a=portrait("A");var c=portrait("C");var screen=client.gui.screen();screen.mouseClicked(left(a.x()+a.width()/2.,a.y()+a.height()/2.),false);screen.mouseDragged(left(c.x()+c.width(),c.y()),100,0);screen.keyPressed(new net.minecraft.client.input.KeyEvent(InputConstants.KEY_ESCAPE,0,0));screen.mouseReleased(left(c.x(),c.y()));if(commands.size()!=4)throw new AssertionError("Escape must cancel the card gesture without another command");});
        game.runOnClient(client->{view[0]=ordered(attached(view[0],"A","G1"),List.of("B","A"));MapCommanderHud.accept(view[0]);});game.waitTicks(2);
        game.runOnClient(client->{
            MapCommanderPanels.select("A",false);
            var a=portrait("A");var screen=client.gui.screen();
            var from=left(a.x()+a.width()/2.,a.y()+a.height()/2.);var to=left(from.x()+1000,from.y());
            screen.mouseClicked(from,false);screen.mouseDragged(to,to.x()-from.x(),0);screen.mouseReleased(to);
            assertCommand(commands,5,"FORMATION/MOVE_ARMIES",List.of("target/G2","A"));
            ack();MapCommanderHud.accept(view[0]);
            view[0]=ordered(attached(view[0],"A","G2"),List.of("C","A"));MapCommanderHud.accept(view[0]);
            if(commands.size()!=5)throw new AssertionError("A rejected membership response must discard the queued order even if a later HUD changes the parent");
        });
        game.runOnClient(client->{MapCommanderHud.reset();MapCommanderOrders.commandObserver=null;MapCommanderOrders.selectionObserver=null;});
    }
    private static void assertCommand(List<ProtocolCommanderOrders.Command> commands,int count,String action,List<String> ids){if(commands.size()!=count||!commands.getLast().action().equals(action)||!commands.getLast().selection().equals(ids)||!commands.getLast().rays().isEmpty())throw new AssertionError("Unexpected drag command "+action+" "+ids+": "+commands);}
}
