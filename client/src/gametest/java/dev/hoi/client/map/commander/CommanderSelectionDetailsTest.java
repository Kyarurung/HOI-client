package dev.hoi.client.map.commander;

import dev.hoi.client.ClientCampaignHud;
import dev.hoi.client.screen.map.MapCursor;
import dev.hoi.protocol.commander.CommanderHudPayloads.*;
import dev.hoi.protocol.commander.CommanderOrderPayloads.*;
import dev.hoi.protocol.hud.*;
import dev.hoi.protocol.menu.*;
import dev.hoi.protocol.unit.*;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

final class CommanderSelectionDetailsTest {
    private CommanderSelectionDetailsTest() {}
    private static int scroll(){try{var f=CommanderPanels.class.getDeclaredField("scroll");f.setAccessible(true);return f.getInt(null);}catch(ReflectiveOperationException error){throw new AssertionError(error);}}
    private static Set<String> rows(){return CommanderPanels.selectedDivisionRows().stream().map(Member::id).collect(java.util.stream.Collectors.toSet());}
    static void run(ClientGameTestContext game,MenuView menu){
        branches(game,menu);
        var selections=new ArrayList<Selection>();var sounds=new ArrayList<String>();var issued=new Context[1];var source=new View[1];
        var members=java.util.stream.IntStream.range(0,64).mapToObj(i->new Member(String.format(Locale.ROOT,"d%03d",i),"Division "+i,"대기 중","unit_hud/infantry","소속: A",.8,new Open(Mode.ARMY_UNITS,"A",List.of("unit/"+String.format(Locale.ROOT,"d%03d",i))))).toList();
        net.minecraft.client.sounds.SoundEventListener listener=(sound,event,range)->{if(sound.getIdentifier().getPath().equals("ui.division.select"))sounds.add("select");};
        try{
            game.getInput().resizeWindow(1280,720);
            game.runOnClient(client->{client.options.guiScale().set(2);CommanderHud.reset();CommanderUnitHud.reset();ClientCampaignHud.accept(HudPayloads.State.of("KOR",menu.hud()));CommanderOrders.selectionObserver=selections::add;CommanderOrders.commandObserver=c->{};client.getSoundManager().addListener(listener);source[0]=new View("KOR",client.level.dimension().identifier().toString(),Mode.ARMY,List.of(new Card("A","Army A","","politics/empty/leader","","",64,24,"",Mode.ASSIGN_ARMY,new Panel("","",null,members.subList(0,1),64,List.of(),members.stream().map(Member::id).toList()))));CommanderHud.accept(source[0]);issued[0]=new Context(UUID.randomUUID().toString(),0,"KOR",source[0].dimension(),"");CommanderOrders.accept(issued[0]);});
            game.setScreen(MapCursor::new);game.waitTicks(2);
            game.runOnClient(client->{var card=CommanderHud.boxes().stream().filter(b->b.request()!=null&&b.request().mode()==Mode.ASSIGN_ARMY).findFirst().orElseThrow();CommanderHud.clickAt(card.x()+card.width()/2.,card.y()+card.height()/2.);if(sounds.size()!=1)throw new AssertionError("A physical Army card selection plays its selection sound once");var sequence=selections.getLast().sequence();CommanderOrders.acceptDetails(new DetailPage(issued[0].token(),sequence,1,32,64,members.subList(32,64)));if(rows().size()!=1)throw new AssertionError("Partial pages cannot publish incomplete selection details");CommanderOrders.acceptDetails(new DetailPage(issued[0].token(),sequence,1,0,64,members.subList(0,32)));if(rows().size()!=64||CommanderPanels.commandSelection().size()!=64)throw new AssertionError("Full authoritative selection details include members beyond the card's limited preview");});game.waitTicks(2);
            var previousScroll=new int[1];
            game.runOnClient(client->{var row=CommanderPanels.hits().stream().filter(h->h.tip().contains("Division 0§!\n")).findFirst().orElseThrow();CommanderHud.scrollAt(row.x()+1,row.y()+1,-1);previousScroll[0]=scroll();if(previousScroll[0]==0)throw new AssertionError("Actual panel wheel must scroll the selected rows");});game.waitTicks(45);
            game.runOnClient(client->{CommanderHud.accept(source[0]);CommanderOrders.accept(issued[0]);if(scroll()!=previousScroll[0]||rows().size()!=64||sounds.size()!=1)throw new AssertionError("Delayed refresh retains the selection and scroll without replaying audio");});game.waitTicks(2);
            game.runOnClient(client->{var row=CommanderPanels.hits().stream().filter(h->h.tip().contains("Division 3§!\n")).findFirst().orElseThrow();CommanderHud.clickAt(row.x()+1,row.y()+1);if(sounds.size()!=2||!rows().equals(Set.of("d003")))throw new AssertionError("Individual left click selects and sounds exactly once without widening to its Army");});game.waitTicks(2);
            game.runOnClient(client->{if(CommanderPanels.hits().stream().filter(h->h.tip().contains("Division ")).count()<=1||!CommanderPanels.commandSelection().equals(List.of("unit/d003")))throw new AssertionError("The army list must retain unselected rows while commands target only the selected division");});game.takeScreenshot("hoi-army-list-partial-selection-gui2");
            game.runOnClient(client->{CommanderUnitHud.selectDivisions(List.of("d003","d010"),false);long seq=selections.getLast().sequence();CommanderOrders.acceptDetails(new DetailPage(issued[0].token(),seq-1,2,0,1,List.of(members.get(0))));if(!rows().equals(Set.of("d003","d010")))throw new AssertionError("A delayed previous selection cannot replace current IDs");CommanderOrders.acceptDetails(new DetailPage(issued[0].token(),seq,3,0,2,List.of(members.get(3),members.get(10))));CommanderHud.accept(source[0]);if(!rows().equals(Set.of("d003","d010"))||sounds.size()!=2)throw new AssertionError("Multi-selection refresh preserves only selected IDs and never plays selection audio");});game.waitTicks(2);game.takeScreenshot("hoi-selected-divisions-only-gui2");
            game.runOnClient(client->{long seq=selections.getLast().sequence();CommanderOrders.acceptDetails(new DetailPage(issued[0].token(),seq,4,0,0,List.of()));if(!CommanderPanels.commandSelection().isEmpty()||!rows().isEmpty())throw new AssertionError("Authoritative loss of all selected units clears their private details");CommanderUnitHud.replaceKeys(List.of("stack|infantry"));CommanderPanels.mapSelectionChanged();var loose=new Member("loose","Unassigned division","대기 중","unit_hud/infantry","미배속",1,new Open(Mode.ARMY_UNITS,"",List.of("unit/loose")));CommanderOrders.acceptDetails(new DetailPage(issued[0].token(),selections.getLast().sequence(),5,0,1,List.of(loose)));if(!CommanderPanels.commandSelection().equals(List.of("unit/loose"))||!rows().equals(Set.of("loose"))||!CommanderPanels.selectedCards().isEmpty())throw new AssertionError("An unassigned map selection receives its stable ID and own detail without creating Army membership");});game.waitTicks(2);game.takeScreenshot("hoi-unassigned-selection-detail-gui2");
            game.runOnClient(client->{
                var missing=new Card("missing","Loading Army","","politics/empty/leader","","",1,24,"",Mode.ASSIGN_ARMY);
                CommanderHud.accept(new View("KOR",source[0].dimension(),Mode.ARMY,List.of(missing)));CommanderPanels.select(missing.id(),true);
                if(!CommanderPanels.commandSelection().equals(List.of("unit/loose")))throw new AssertionError("Missing card membership cannot erase an explicitly selected unassigned division");
                var old=selections.getLast();CommanderHud.accept(new View("",source[0].dimension(),Mode.ARMY,List.of()));CommanderOrders.acceptDetails(new DetailPage(old.token(),old.sequence(),6,0,1,List.of(members.getFirst())));if(!CommanderPanels.commandSelection().isEmpty()||!rows().isEmpty()||CommanderOrders.ready())throw new AssertionError("Revoked lobby context rejects late selection detail pages");
            });
        }finally{game.runOnClient(client->{client.getSoundManager().removeListener(listener);CommanderOrders.commandObserver=null;CommanderOrders.selectionObserver=null;CommanderHud.reset();CommanderUnitHud.reset();});}
    }
    private static void branches(ClientGameTestContext game,MenuView menu){
        String directory=System.getenv("HOI_COMMANDER_PREVIEW");if(directory==null)return;
        try{for(String branch:List.of("navy","air")){
            var source=new Snapshot(java.nio.file.Files.readString(java.nio.file.Path.of(directory,"kor-command-panel-"+branch+".json"))).view();boolean navy=branch.equals("navy");var member=navy?source.cards().getFirst().panel().members().getFirst():source.wings().getFirst();
            String key=navy?member.id()+"|navy_destroyer":member.selectionKey();if(key.isEmpty())throw new AssertionError("Actual branch counter key is required");
            var played=new ArrayList<String>();net.minecraft.client.sounds.SoundEventListener listener=(sound,event,range)->{if(sound.getIdentifier().getPath().equals(navy?"ui.fleet.select":"ui.aircraft.select"))played.add("select");};
            game.runOnClient(client->{CommanderHud.reset();CommanderUnitHud.reset();ClientCampaignHud.accept(HudPayloads.State.of("KOR",menu.hud()));CommanderOrders.selectionObserver=x->{};CommanderOrders.commandObserver=x->{};CommanderHud.accept(new View("KOR",client.level.dimension().identifier().toString(),source.branch(),source.cards(),source.wings()));CommanderOrders.accept(new Context(UUID.randomUUID().toString(),0,"KOR",client.level.dimension().identifier().toString(),""));client.player.setXRot(0);client.player.setYRot(0);var at=client.player.getEyePosition().add(0,0,8);int split=key.lastIndexOf('|');CommanderUnitHud.accept(new UnitHud.Snapshot(client.level.dimension().identifier().toString(),List.of(new UnitHud.Counter(key.substring(0,split),"KOR","한국",key.substring(split+1),member.name(),1,at.x,at.y,at.z,1,1,1,Map.of()))));client.getSoundManager().addListener(listener);});
            try{
                game.setScreen(MapCursor::new);game.waitTicks(2);
                game.runOnClient(client->{var box=CommanderUnitHud.boxes().getFirst();if(!CommanderUnitHud.selectAt(box.x()+box.width()/2.,box.y()+box.height()/2.)||played.size()!=1)throw new AssertionError("Actual "+branch+" counter left click must play selection sound exactly once");var ids=CommanderPanels.commandSelection();if(navy&&!ids.equals(List.of("navy/"+member.id()))||!navy&&(ids.isEmpty()||ids.stream().anyMatch(id->!id.startsWith("air/")||id.contains("|"))))throw new AssertionError("Map counter selection must resolve to stable owned branch IDs: "+ids);});
                var ids=game.computeOnClient(client->CommanderPanels.commandSelection());game.waitTicks(45);
                game.runOnClient(client->{CommanderHud.accept(new View("KOR",client.level.dimension().identifier().toString(),source.branch(),source.cards(),source.wings()));if(!ids.equals(CommanderPanels.commandSelection())||played.size()!=1)throw new AssertionError("Branch details and stable IDs survive delayed refresh without audio replay");});game.waitTicks(2);
                game.runOnClient(client->{if(CommanderPanels.hits().isEmpty())throw new AssertionError("Selected branch must retain its left details");});game.takeScreenshot("hoi-map-selected-"+branch+"-details-gui2");
                if(!navy)game.runOnClient(client->{var replacement=new Member("new-wing",member.name(),member.status(),member.icon(),member.detail(),member.strength(),new Open(Mode.AIR_MISSION,"new-wing",List.of()),key,member.organization(),member.composition(),member.group(),member.mission(),member.data());var updated=new ArrayList<>(source.wings());updated.add(replacement);CommanderHud.accept(new View("KOR",client.level.dimension().identifier().toString(),source.branch(),source.cards(),updated));if(!ids.equals(CommanderPanels.commandSelection()))throw new AssertionError("A new wing sharing a previous counter must not join an existing stable selection");CommanderHud.accept(new View("KOR",client.level.dimension().identifier().toString(),source.branch(),source.cards(),List.of(replacement)));if(!CommanderPanels.commandSelection().isEmpty()||CommanderPanels.counterSelected(key))throw new AssertionError("A replaced air counter cannot inherit the removed wing's selection or highlight");});
                if(navy)game.runOnClient(client->{
                    var card=source.cards().getFirst();var panel=card.panel();var partial=new Panel(panel.parent(),panel.commander(),panel.leader(),List.of(),panel.total(),panel.controls());
                    var changed=new ArrayList<>(source.cards());changed.set(0,new Card(card.id(),card.name(),card.leader(),card.portrait(),card.marshal(),card.marshalPortrait(),card.count(),card.capacity(),card.detail(),card.role(),partial));
                    CommanderHud.accept(new View("KOR",client.level.dimension().identifier().toString(),source.branch(),changed,List.of()));
                    if(!CommanderPanels.commandSelection().equals(ids))throw new AssertionError("Incomplete fleet membership cannot delete an explicitly selected map force");
                    CommanderHud.accept(new View("KOR",client.level.dimension().identifier().toString(),source.branch(),source.cards(),List.of()));
                });
                game.runOnClient(client->{CommanderHud.accept(new View("KOR",client.level.dimension().identifier().toString(),source.branch(),navy?List.of():source.cards(),List.of()));if(!CommanderPanels.commandSelection().isEmpty()||CommanderPanels.counterSelected(key))throw new AssertionError("Removed branch selection must not widen or retain a stale counter highlight");});
            }finally{game.runOnClient(client->client.getSoundManager().removeListener(listener));}
        }}catch(java.io.IOException error){throw new AssertionError(error);}finally{game.runOnClient(client->{CommanderOrders.commandObserver=null;CommanderOrders.selectionObserver=null;CommanderHud.reset();CommanderUnitHud.reset();});}
    }

}
