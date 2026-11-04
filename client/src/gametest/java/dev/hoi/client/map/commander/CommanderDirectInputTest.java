package dev.hoi.client.map.commander;

import com.mojang.blaze3d.platform.InputConstants;
import dev.hoi.client.ClientCampaignHud;
import dev.hoi.client.screen.map.MapCursor;
import dev.hoi.client.ui.UiCommandLabels;
import dev.hoi.protocol.commander.CommanderHudPayloads.*;
import dev.hoi.protocol.commander.*;
import dev.hoi.protocol.hud.*;
import dev.hoi.protocol.menu.*;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

final class CommanderDirectInputTest {
    private CommanderDirectInputTest() {}
    static void run(ClientGameTestContext context,MenuView menu,String directory)throws java.io.IOException {
        var actions=new CommanderOrderPayloads.Availability(Files.readString(Path.of(directory,"kor-navy-assessment.json"))).assessment().actions();
        for(String kind:List.of("navy","air-attached","army-air-attached")){
            var source=new Snapshot(Files.readString(Path.of(directory,"kor-command-panel-"+kind+".json"))).view();boolean navy=source.branch()==Mode.NAVY_HQ;
            context.getInput().resizeWindow(1280,720);
            context.runOnClient(client->{client.options.guiScale().set(2);ClientCampaignHud.accept(HudPayloads.State.of(source.country(),menu.hud()));CommanderHud.requestObserver=null;CommanderHud.reset();CommanderUnitHud.reset();CommanderHud.accept(new View(source.country(),client.level.dimension().identifier().toString(),source.branch(),source.cards(),source.wings()));});
            context.setScreen(MapCursor::new);context.waitTicks(2);
            var target=new String[1];var armySelection=new ArrayList<String>();var armySkills=new ArrayList<String>();
            if(source.branch()==Mode.ARMY){
                context.runOnClient(client->{var attached=source.wings().stream().filter(wing->wing.data()!=null&&!wing.data().attachedArmy().isEmpty()).findFirst().orElseThrow();clickArmy(attached.data().attachedArmy());});context.waitTicks(2);
                context.runOnClient(client->{armySelection.addAll(CommanderPanels.commandSelection());armySkills.addAll(skills());if(armySelection.isEmpty()||!armySkills.equals(CommanderPanels.armyAbilityIds().stream().map(id->"ability_"+id).toList())||!CommanderPanels.armyInput())throw new AssertionError("The actual general card must select its divisions and show its authoritative skills before entering attached air: "+armySelection+" "+armySkills+" "+CommanderPanels.armyInput());});
            }
            if(source.branch()==Mode.ARMY){context.runOnClient(client->{if(CommanderHud.boxes().stream().anyMatch(box->box.request()!=null&&box.request().mode()==Mode.AIR_ATTACH)||CommanderPanels.hits().stream().noneMatch(hit->hit.icon().equals("army_select_wings_btn")))throw new AssertionError("Default map must hide attachment slots and retain the header control");});context.takeScreenshot("hoi-army-attached-wings-hidden-1280x720-gui2");context.runOnClient(client->{CommanderHud.reset();CommanderUnitHud.reset();});continue;}
            context.runOnClient(client->{var screen=client.gui.screen();List<String> expected;
                if(navy){var counter=CommanderHud.boxes().stream().filter(box->box.request()!=null&&box.request().mode()==Mode.NAVAL_TASK_FORCES).findFirst().orElseThrow();target[0]=counter.request().target();CommanderHud.clickAt(counter.x()+counter.width()/2.,counter.y()+counter.height()/2.);expected=source.cards().stream().filter(card->card.id().equals(target[0])).findFirst().orElseThrow().panel().members().stream().map(force->"navy/"+force.id()).sorted().toList();}
                else {var attached=source.wings().stream().filter(wing->wing.data()!=null&&!wing.data().attachedArmy().isEmpty()).toList();if(attached.size()!=1)throw new AssertionError("The actual attached fixture must contain one wing");target[0]=attached.getFirst().id();var previousSkill=CommanderPanels.hits().stream().filter(hit->hit.icon().startsWith("ability_")).findFirst().orElse(null);var band=CommanderHud.boxes().stream().filter(box->box.action()!=null&&(source.branch()==Mode.AIR?box.request()!=null&&box.request().mode()==Mode.AIR_ATTACH&&box.request().selection().equals(List.of("action/detach",target[0]))&&box.tip().startsWith(UiCommandLabels.text("AIRWING_ATTACHED","missing")):box.tip().equals(UiCommandLabels.text("SELECT_ATTACHED_AIRWINGS","missing")))).findFirst().orElseThrow();if(!CommanderHud.clickAt(band.x()+band.width()/2.,band.y()+band.height()/2.))throw new AssertionError("The actual attachment control must consume its click");expected=List.of("air/"+target[0]);if(previousSkill!=null)retiredHit(previousSkill,source.country());}
                if(client.gui.screen()!=screen||!CommanderPanels.commandSelection().equals(expected))throw new AssertionError("Actual card or attachment control must open the native command panel with its exact units: "+kind);
            });context.waitTicks(2);context.takeScreenshot("hoi-direct-mission-entry-"+kind+"-gui2");
            if(source.branch()==Mode.ARMY)blockedArmy(context,source);
            if(navy){
                var card=source.cards().stream().filter(value->value.id().equals(target[0])).findFirst().orElseThrow();var force=card.panel().members().getFirst();var state=force.data().navy();
                context.runOnClient(client->{var screen=client.gui.screen();if(!CommanderOrders.route(new Open(Mode.NAVAL_MISSION,force.id()))||client.gui.screen()!=screen||!CommanderPanels.commandSelection().equals(List.of("navy/"+force.id())))throw new AssertionError("Task-force detail must select exactly one force in the native panel");CommanderOrders.selectionObserver=selection->{};CommanderAvailability.queryObserver=query->CommanderAvailability.accept(new CommanderOrderPayloads.Assessment(query.token(),query.revision(),query.sequence(),query.request(),actions));CommanderOrders.accept(new CommanderOrderPayloads.Context(UUID.randomUUID().toString(),0,source.country(),client.level.dimension().identifier().toString(),""));});context.waitTicks(2);
                context.runOnClient(client->{
                    var split=find("NAVY_REPAIR_POLICY_SPLIT_DESC");CommanderPanelsTest.directClick(split,"NAVAL_SETTING/AUTO_SPLIT",List.of(force.id(),"state/"+(state.autoSplit()==null?"unset":state.autoSplit())),source.country());
                    if(CommanderPanels.hits().stream().anyMatch(hit->!hit.icon().equals("naval_tool_REINFORCE")&&hit.tip().startsWith(UiCommandLabels.text("TASK_FORCE_AUTO_REINFORCEMENT_DESC","missing"))))throw new AssertionError("Task-force rows must not expose the removed individual reinforcement control");
                    var reinforce=CommanderPanels.hits().stream().filter(hit->hit.icon().equals("naval_tool_REINFORCE")).findFirst().orElseThrow();var decision=actions.get("NAVAL_TB/REINFORCE");if(!decision.enabled()||!reinforce.tip().equals(decision.details()))throw new AssertionError("Reinforcement toolbar must retain the enabled authoritative tooltip");CommanderPanelsTest.directClick(reinforce,"NAVAL_SETTING/BATCH/REINFORCE",List.of(force.id(),"0/state/"+(state.reinforce()==null?"unset":state.reinforce())),source.country());
                    var engagement=CommanderPanels.hits().stream().filter(hit->hit.tip().contains("클릭: 다음 교전 규칙")).findFirst().orElseThrow();CommanderPanelsTest.directClick(engagement,"NAVAL_SETTING/ENGAGEMENT_NEXT",List.of(force.id(),"engagement/"+state.engagement()),source.country());
                    CommanderPanelsTest.directClick(engagement,"NAVAL_SETTING/ENGAGEMENT_PREVIOUS",List.of(force.id(),"engagement/"+state.engagement()),source.country(),com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT);
                    CommanderHud.open(new Open(Mode.NAVAL_TASK_FORCES,card.id()));if(!CommanderPanels.commandSelection().equals(List.of("navy/"+force.id())))throw new AssertionError("Reopening a selected fleet must retain the chosen task-force subset");
                });
            }else{
                var wing=source.wings().stream().filter(value->value.id().equals(target[0])).findFirst().orElseThrow();
                context.runOnClient(client->{var screen=client.gui.screen();var previousSortie=CommanderPanels.hits().stream().filter(hit->hit.icon().equals("strategicair_day_night")).findFirst().orElseThrow();var row=airRow(wing);if(!CommanderHud.clickAt(row.x()+row.w()/2.,row.y()+row.h()/2.)||client.gui.screen()!=screen||!CommanderPanels.commandSelection().equals(List.of("air/"+wing.id())))throw new AssertionError("Clicking the actual wing row must retain its exact air selection");retiredHit(previousSortie,source.country());});context.waitTicks(2);
                context.runOnClient(client->{find("AIRWING_MISSION_TYPE_AIR_SUPERIORITY");airRow(wing);if(CommanderPanels.armyInput()||!CommanderPanels.commandSelection().equals(List.of("air/"+wing.id())))throw new AssertionError("The wing row must keep its native air mission panel after rendering");var screen=client.gui.screen();var detail=CommanderPanels.hits().stream().filter(hit->hit.icon().equals("unitlist_unitinfo_button")).findFirst().orElseThrow();var previousSortie=CommanderPanels.hits().stream().filter(hit->hit.icon().equals("strategicair_day_night")).findFirst().orElseThrow();if(!CommanderHud.clickAt(detail.x()+detail.w()/2.,detail.y()+detail.h()/2.)||client.gui.screen()!=screen||!CommanderPanels.commandSelection().equals(List.of("air/"+wing.id())))throw new AssertionError("The actual wing detail click must reuse its native mission panel without a second route call");retiredHit(previousSortie,source.country());});context.waitTicks(2);
                context.runOnClient(client->{var sortie=CommanderPanels.hits().stream().filter(hit->hit.icon().equals("strategicair_day_night")).findFirst().orElseThrow();CommanderPanelsTest.directClick(sortie,"AIR_SETTING/SORTIE_NEXT",List.of(wing.id(),"sortie/"+wing.data().air().wing().sortieTime()),source.country());});
            }
            context.waitTicks(2);context.takeScreenshot("hoi-direct-mission-settings-"+kind+"-gui2");
            if(navy){try{missingNavalState(context,source);}finally{context.runOnClient(client->{CommanderAvailability.queryObserver=null;CommanderOrders.reset();CommanderOrders.selectionObserver=null;});}}else airTarget(context,source,target[0]);
            if(source.branch()==Mode.ARMY){context.waitTicks(2);returnToArmy(context,source,target[0],armySelection,armySkills);}
            context.runOnClient(client->{CommanderHud.reset();CommanderUnitHud.reset();});
        }
    }
    private static List<String> skills(){return CommanderPanels.hits().stream().map(CommanderPanels.Hit::icon).filter(icon->icon.startsWith("ability_")).toList();}
    private static CommanderPanels.Hit airRow(Member wing){return CommanderPanels.hits().stream().filter(hit->hit.sound().equals("ui.aircraft.select")&&hit.tip().equals("§e"+wing.name()+"§r\n"+wing.detail())).findFirst().orElseThrow();}
    private static void clickArmy(String army){var box=CommanderHud.boxes().stream().filter(value->value.request()!=null&&value.request().mode()==Mode.ASSIGN_ARMY&&value.request().target().equals(army)).findFirst().orElseThrow();if(!CommanderHud.clickAt(box.x()+box.width()/2.,box.y()+box.height()/2.))throw new AssertionError("The actual army portrait must consume its selection click");}
    private static void retiredHit(CommanderPanels.Hit hit,String country){
        if(!CommanderPanels.hits().isEmpty())throw new AssertionError("Selection transitions must invalidate previous rendered hit closures immediately");
        var client=net.minecraft.client.Minecraft.getInstance();var previous=CommanderHud.requestObserver;var commands=new ArrayList<CommanderOrderPayloads.Command>();var dialogs=new ArrayList<Open>();var selection=CommanderPanels.commandSelection();var screen=client.gui.screen();
        CommanderHud.requestObserver=dialogs::add;CommanderOrders.commandObserver=commands::add;CommanderOrders.selectionObserver=value->{};
        try{CommanderOrders.accept(new CommanderOrderPayloads.Context(UUID.randomUUID().toString(),0,country,client.level.dimension().identifier().toString(),""));for(int button:List.of(InputConstants.MOUSE_BUTTON_LEFT,InputConstants.MOUSE_BUTTON_RIGHT))if(CommanderHud.clickAt(hit.x()+hit.w()/2.,hit.y()+hit.h()/2.,button,false))throw new AssertionError("A retired panel coordinate must not run an old closure before the next render");if(!commands.isEmpty()||!dialogs.isEmpty()||client.gui.screen()!=screen||!CommanderPanels.commandSelection().equals(selection))throw new AssertionError("Retired hits cannot issue requests or change the current selection");}
        finally{CommanderOrders.reset();CommanderOrders.commandObserver=null;CommanderOrders.selectionObserver=null;CommanderHud.requestObserver=previous;}
    }
    private static void blockedArmy(ClientGameTestContext context,View source){
        context.runOnClient(client->{
            if(!skills().isEmpty()||CommanderPanels.armyCommandVisible()||CommanderPanels.hits().stream().anyMatch(h->h.icon().startsWith("bpt_")||h.icon().equals("exercise_icon")))throw new AssertionError("Attached air selection must hide army skills and command hits");
            var commands=new ArrayList<CommanderOrderPayloads.Command>();var dialogs=new ArrayList<Open>();var previous=CommanderHud.requestObserver;var selected=CommanderPanels.commandSelection();var cards=CommanderPanels.selectedCards();var screen=client.gui.screen();
            CommanderHud.requestObserver=dialogs::add;CommanderOrders.commandObserver=commands::add;CommanderOrders.selectionObserver=value->{};
            try{CommanderOrders.accept(new CommanderOrderPayloads.Context(UUID.randomUUID().toString(),0,source.country(),client.level.dimension().identifier().toString(),"LAND/AREA_STATES"));var units=CommanderLayout.units(client.getWindow().getGuiScaledWidth(),client.getWindow().getGuiScaledHeight());for(int n=0;n<16;n++){var cell=CommanderLayout.armyCell(units,CommanderLayout.commandLeft(units,false),CommanderPanels.actionbarTop(units,false),n);for(int button:List.of(InputConstants.MOUSE_BUTTON_LEFT,InputConstants.MOUSE_BUTTON_RIGHT))CommanderHud.clickAt(cell.x()+cell.width()/2.,cell.y()+cell.height()/2.,button,false);}CommanderPanels.armyAbility("force_attack");if(CommanderOrders.confirmDraft())throw new AssertionError("Hidden army planning cannot accept its shortcut");if(!commands.isEmpty()||!dialogs.isEmpty()||client.gui.screen()!=screen||!CommanderPanels.commandSelection().equals(selected)||!CommanderPanels.selectedCards().equals(cards))throw new AssertionError("Hidden army controls must not issue requests or change selection while attached air is selected");}
            finally{CommanderOrders.reset();CommanderOrders.commandObserver=null;CommanderOrders.selectionObserver=null;CommanderHud.requestObserver=previous;}
        });context.waitTicks(2);
    }
    private static void returnToArmy(ClientGameTestContext context,View source,String wing,List<String> expectedSelection,List<String> expectedSkills){
        var attached=source.wings().stream().filter(value->value.id().equals(wing)).findFirst().orElseThrow();
        context.runOnClient(client->clickArmy(attached.data().attachedArmy()));context.waitTicks(2);
        context.runOnClient(client->{if(!CommanderPanels.armyInput()||!CommanderPanels.attachmentWings().isEmpty()||!CommanderPanels.commandSelection().equals(expectedSelection)||!skills().equals(expectedSkills)||!CommanderPanels.selectedCards().equals(Set.of(attached.data().attachedArmy())))throw new AssertionError("Returning to the actual general card must restore its divisions and general skills");if(CommanderPanels.hits().stream().anyMatch(hit->hit.tip().contains("비행단을 선택한 상태입니다. 군이나 사단을 선택하세요.")))throw new AssertionError("Returning to army selection must remove the incompatible air-selection reason");});
    }
    private static void airTarget(ClientGameTestContext context,View source,String wing) {
        context.runOnClient(client->{
            var requests=new ArrayList<CommanderOrderPayloads.Command>();var selections=new ArrayList<CommanderOrderPayloads.Selection>();var previous=CommanderHud.requestObserver;
            CommanderHud.requestObserver=null;CommanderOrders.commandObserver=requests::add;CommanderOrders.selectionObserver=selections::add;
            String token=UUID.randomUUID().toString(),dimension=client.level.dimension().identifier().toString();var expected=List.of("air/"+wing);var screen=client.gui.screen();var camera=client.options.getCameraType();float pitch=client.player.getXRot(),yaw=client.player.getYRot();
            try{
                CommanderOrders.accept(new CommanderOrderPayloads.Context(token,0,source.country(),dimension,""));
                if(!CommanderPanels.commandSelection().equals(expected)||CommanderPanels.armyInput())throw new AssertionError("Attached air selection must be independent of land-map army cards");
                var mission=find("AIRWING_MISSION_TYPE_AIR_SUPERIORITY");CommanderHud.clickAt(mission.x()+mission.w()/2.,mission.y()+mission.h()/2.);CommanderHud.clickAt(mission.x()+mission.w()/2.,mission.y()+mission.h()/2.);
                if(requests.size()!=1||!requests.getFirst().action().equals("AIR/SUPERIORITY")||!requests.getFirst().selection().equals(List.of(wing))||!requests.getFirst().rays().isEmpty()||client.gui.screen()!=screen)throw new AssertionError("Attached air mission must directly request exactly its wing without an intermediate dialog");
                CommanderOrders.accept(new CommanderOrderPayloads.Context(token,1,source.country(),dimension,"AIR/SUPERIORITY"));
                CommanderHud.accept(new View(source.country(),dimension,source.branch(),source.cards(),source.wings()));
                if(!CommanderPanels.commandSelection().equals(expected)||!CommanderOrders.pending().equals("AIR/SUPERIORITY")||!CommanderUnitHud.selectedKeys().isEmpty())throw new AssertionError("Snapshot refresh must preserve pure air selection and pending target on the army map");
                client.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);client.player.setXRot(60);client.player.setYRot(0);
                double x=screen.width*.78,y=screen.height*.75;var right=new net.minecraft.client.input.MouseButtonEvent(x,y,new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT,0));
                screen.mouseClicked(right,false);screen.mouseReleased(right);screen.mouseReleased(right);
                if(requests.size()!=2||!requests.getLast().action().equals("MAP")||!requests.getLast().selection().equals(expected)||requests.getLast().rays().size()!=1||client.gui.screen()!=screen)throw new AssertionError("Attached air must reach real map right-click routing with exactly one ray and its published selection: "+requests);
                CommanderOrders.accept(new CommanderOrderPayloads.Context(token,2,source.country(),dimension,"AIR/SUPERIORITY"));
                if(!CommanderOrders.cancelTarget()||!requests.getLast().action().equals("CANCEL"))throw new AssertionError("Attached air targeting must remain cancellable after a server response");
                if(selections.isEmpty()||!selections.getLast().keys().equals(expected))throw new AssertionError("Attached air exact selection must be published before mission input");
            }finally{client.options.setCameraType(camera);client.player.setXRot(pitch);client.player.setYRot(yaw);CommanderOrders.reset();CommanderOrders.commandObserver=null;CommanderOrders.selectionObserver=null;CommanderHud.requestObserver=previous;}
        });
    }
    private static void missingNavalState(ClientGameTestContext context,View source) {
        var card=source.cards().stream().filter(value->value.panel()!=null&&!value.panel().members().isEmpty()).findFirst().orElseThrow();var force=card.panel().members().getFirst();
        for(boolean missingMember:List.of(true,false)){
            var data=force.data();var absent=missingMember?null:new MemberData(data.template(),data.attachedArmy(),data.quantity(),data.insignia(),null,data.reinforce(),data.air(),data.strategic());
            var replacement=new Member(force.id(),force.name(),force.status(),force.icon(),force.detail(),force.strength(),force.request(),force.selectionKey(),force.organization(),force.composition(),force.group(),force.mission(),absent);
            var panel=card.panel();var changed=new Panel(panel.parent(),panel.commander(),panel.leader(),panel.members().stream().map(member->member.id().equals(force.id())?replacement:member).toList(),panel.total(),panel.controls(),panel.divisionIds(),panel.color(),panel.palette());
            var replacementCard=new Card(card.id(),card.name(),card.leader(),card.portrait(),card.marshal(),card.marshalPortrait(),card.count(),card.capacity(),card.detail(),card.role(),changed);
            context.runOnClient(client->{CommanderHud.reset();CommanderHud.accept(new View(source.country(),client.level.dimension().identifier().toString(),source.branch(),source.cards().stream().map(value->value.id().equals(card.id())?replacementCard:value).toList(),source.wings()));if(!CommanderOrders.route(new Open(Mode.NAVAL_MISSION,force.id())))throw new AssertionError("Known force with missing state must remain selectable");});context.waitTicks(2);
            context.runOnClient(client->{var target=find("NAVAL_BTN_SET_TARGET");CommanderHud.clickAt(target.x()+target.w()/2.,target.y()+target.h()/2.);});context.waitTicks(2);
            context.runOnClient(client->{
                var requests=new ArrayList<CommanderOrderPayloads.Command>();var dialogs=new ArrayList<Open>();var previous=CommanderHud.requestObserver;CommanderHud.requestObserver=dialogs::add;CommanderOrders.commandObserver=requests::add;CommanderOrders.selectionObserver=selection->{};
                try{
                    CommanderOrders.accept(new CommanderOrderPayloads.Context(UUID.randomUUID().toString(),0,source.country(),client.level.dimension().identifier().toString(),""));var screen=client.gui.screen();
                    var controls=CommanderPanels.hits().stream().filter(hit->hit.tip().contains("태스크 포스 설정을 불러오는 중입니다.")).toList();if(controls.size()!=11)throw new AssertionError("Missing naval state must explain auto-split and engagement settings and nine target classes");
                    for(var hit:controls){CommanderHud.clickAt(hit.x()+hit.w()/2.,hit.y()+hit.h()/2.);CommanderHud.clickAt(hit.x()+hit.w()/2.,hit.y()+hit.h()/2.,com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT,false);}
                    var reinforce=CommanderPanels.hits().stream().filter(hit->hit.icon().equals("naval_tool_REINFORCE")).findFirst().orElseThrow();if(!CommanderAvailability.reason("NAVAL_TB/REINFORCE").isEmpty())throw new AssertionError("Missing-state reinforcement check must reach the empty batch guard with an enabled server assessment");for(int button:List.of(InputConstants.MOUSE_BUTTON_LEFT,InputConstants.MOUSE_BUTTON_RIGHT))CommanderHud.clickAt(reinforce.x()+reinforce.w()/2.,reinforce.y()+reinforce.h()/2.,button,false);
                    if(!requests.isEmpty()||!dialogs.isEmpty()||client.gui.screen()!=screen||!CommanderPanels.commandSelection().equals(List.of("navy/"+force.id())))throw new AssertionError("Missing naval state must not send guessed settings, open fallback dialogs or broaden selection");
                }finally{CommanderOrders.reset();CommanderOrders.commandObserver=null;CommanderOrders.selectionObserver=null;CommanderHud.requestObserver=previous;}
            });
        }
    }
    private static CommanderPanels.Hit find(String key){String title=UiCommandLabels.text(key,"missing");return CommanderPanels.hits().stream().filter(hit->hit.tip().startsWith(title)).findFirst().orElseThrow();}
}
